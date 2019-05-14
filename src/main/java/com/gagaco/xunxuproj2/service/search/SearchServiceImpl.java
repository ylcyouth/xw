package com.gagaco.xunxuproj2.service.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gagaco.xunxuproj2.base.HouseSort;
import com.gagaco.xunxuproj2.base.RentValueBlock;
import com.gagaco.xunxuproj2.entity.House;
import com.gagaco.xunxuproj2.entity.HouseDetail;
import com.gagaco.xunxuproj2.entity.HouseTag;
import com.gagaco.xunxuproj2.repository.HouseDetailRepository;
import com.gagaco.xunxuproj2.repository.HouseRepository;
import com.gagaco.xunxuproj2.repository.HouseTagRepository;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.web.form.RentSearch;
import com.google.common.primitives.Longs;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @date 2019-5-2 00:12:17
 * @author wangjiajia
 *
 */
@Service
public class SearchServiceImpl implements ISearchService {

    private Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    /**
     * 如果实现没有创建要使用的topic的话，启动的时候会报这个提示
     * 2019-05-12 23:21:27.314  INFO 18252 --- [  restartedMain] o.a.kafka.common.utils.AppInfoParser     : Kafka version : 0.10.1.1
     * 2019-05-12 23:21:27.314  INFO 18252 --- [  restartedMain] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId : f10ef2720b03b247
     * 2019-05-12 23:21:27.373  INFO 18252 --- [  restartedMain] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 33033 (http)
     * 2019-05-12 23:21:27.477  INFO 18252 --- [  restartedMain] com.gagaco.xunxuproj2.Application        : Started Application in 12.563 seconds (JVM running for 14.09)
     * 2019-05-12 23:21:27.563  WARN 18252 --- [ntainer#0-0-C-1] org.apache.kafka.clients.NetworkClient   : Error while fetching metadata with correlation id 1 : {house_build=LEADER_NOT_AVAILABLE}
     * 2019-05-12 23:21:27.565  INFO 18252 --- [ntainer#0-0-C-1] o.a.k.c.c.internals.AbstractCoordinator  : Discovered coordinator centos14:9092 (id: 2147483647 rack: null) for group xunwu.
     * 2019-05-12 23:21:38.848  INFO 18252 --- [ntainer#0-0-C-1] o.a.k.c.c.internals.AbstractCoordinator  : Marking the coordinator centos14:9092 (id: 2147483647 rack: null) dead for group xunwu
     */
    private static final String INDEX_NAME = "xunwu";

    private static final String INDEX_TYPE = "house";

    private static final String INDEX_TOPIC = "xunwubuild";

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransportClient client;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    //topics不写会报一个错
    @KafkaListener(topics = INDEX_TOPIC)
    private void handleMessage(String content) {
        try {
            HouseIndexMessage message = objectMapper.readValue(content, HouseIndexMessage.class);
            switch (message.getOperation()) {
                case HouseIndexMessage.INDEX:
                    this.createOrUpdateIndex(message);
                    break;
                case HouseIndexMessage.REMOVE:
                    this.removeIndex(message);
                    break;
                default:
                    logger.warn("Not support message content " + content);
                    break;
            }
        } catch (IOException e) {
            logger.error("Cannot parse json for " + content);
        }
    }



    private void createOrUpdateIndex(HouseIndexMessage message) {
        long houseId = message.getHouseId();

        House house = houseRepository.findOne(houseId);
        if (house == null) {
            logger.error("Index house {} dose not exist " + houseId);
            //return;
            this.index(houseId, message.getRetry() + 1);
        }

        HouseIndexTemplate template = new HouseIndexTemplate();
        modelMapper.map(house, template);

        HouseDetail detail = houseDetailRepository.findByHouseId(houseId);
        if (detail == null) {
            //todo
        }

        modelMapper.map(detail, template);
        List<HouseTag> tags = houseTagRepository.findAllByHouseId(houseId);
        if (tags != null && !tags.isEmpty()) {
            List<String> tagStrings = new ArrayList<>();
            tags.forEach(tag -> tagStrings.add(tag.getName()));
            template.setTags(tagStrings);
        }

        SearchRequestBuilder builder = client.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE);
        builder.setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        logger.debug(builder.toString());
        SearchResponse searchResponse = builder.get();

        long totalHit = searchResponse.getHits().getTotalHits();

        boolean success;
        if (totalHit == 0) {
            success = create(template);
        } else if (totalHit == 1) {
            String id = searchResponse.getHits().getAt(0).getId();
            success = update(id, template);
        } else {
            success = deleteAndCreate(totalHit, template);
        }

        /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/



        /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

        if (success) {
            logger.debug("Index house success with house: " + houseId);
        }

    }

    public void index(Long houseId) {
        this.index(houseId, 0);
    }

    private void index(long houseId, int retry) {
        if (retry > HouseIndexMessage.MAX_RETRY) {
            logger.error("Retry index times over 3 for house: " + houseId + "" +"Please check it");
            return;
        }

        HouseIndexMessage message = new HouseIndexMessage(houseId, HouseIndexMessage.INDEX, retry);
        try {
            kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            logger.error("Json encode error for " + message);
        }
    }

    public void remove(Long houseId) {
        this.remove(houseId, 0);
    }

    private void remove(Long houseId, int retry) {
        if (retry > HouseIndexMessage.MAX_RETRY) {
            logger.error("Retry remove times over 3 for house: " + houseId + "" +"Please check it");
            return;
        }

        HouseIndexMessage message = new HouseIndexMessage(houseId, HouseIndexMessage.REMOVE, retry);

        try {
            this.kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            logger.error("Json encode error for " + message);
        }
    }

    private void removeIndex(HouseIndexMessage message) {
        Long houseId = message.getHouseId();
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(client);
        builder.filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        builder.source(INDEX_NAME);

        logger.debug("Debug by query for house: " + builder);

        BulkByScrollResponse response = builder.get();
        long deleted = response.getDeleted();
        logger.debug("Delete total " + deleted);

        if (deleted <= 0) {
            this.remove(houseId, message.getRetry() + 1);
        }
    }




   /* @Override
    public void index(Long houseId) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            logger.error("Index house {} does not exit", houseId);
            return;
        }

        HouseIndexTemplate template = new HouseIndexTemplate();
        modelMapper.map(house, template);

        HouseDetail detail = houseDetailRepository.findByHouseId(houseId);
        if (detail == null) {
            //todo 异常情况
        }
        modelMapper.map(detail, template);

        List<HouseTag> tags = houseTagRepository.findAllByHouseId(houseId);
        if (tags != null && !tags.isEmpty()) {
            List<String> tagStrings = new ArrayList<>();
            tags.forEach(tag -> tagStrings.add(tag.getName()));
            template.setTags(tagStrings);
        }

        SearchRequestBuilder builder = client.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE);
        builder.setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        logger.debug(builder.toString());
        SearchResponse searchResponse = builder.get();

        long totalHit = searchResponse.getHits().getTotalHits();

        boolean success;
        if (totalHit == 0) {
            success = create(template);
        } else if (totalHit == 1) {
            String id = searchResponse.getHits().getAt(0).getId();
            success = update(id, template);
        } else {
            success = deleteAndCreate(totalHit, template);
        }

        if (success) {
            logger.debug("Index house success with house: " + houseId);
        }
    }*/

    private boolean create(HouseIndexTemplate template) {
        IndexRequestBuilder indexRequestBuilder = client.prepareIndex(INDEX_NAME, INDEX_TYPE);
        try {
            indexRequestBuilder.setSource(objectMapper.writeValueAsBytes(template), XContentType.JSON);
            IndexResponse indexResponse = indexRequestBuilder.get();
            logger.debug("Create index with house: " + template.getHouseId());
            if (indexResponse.status() == RestStatus.CREATED) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error to index house " + template.getHouseId(), e);
            return false;
        }
    }

    private boolean update(String id, HouseIndexTemplate template) {
        UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(INDEX_NAME, INDEX_TYPE, id);
        try {
            updateRequestBuilder.setDoc(objectMapper.writeValueAsBytes(template), XContentType.JSON);
            UpdateResponse updateResponse = updateRequestBuilder.get();
            logger.debug("Update index with house: " + template.getHouseId());
            if (updateResponse.status() == RestStatus.OK) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error to Update house " + template.getHouseId(), e);
            return false;
        }
    }

    private boolean deleteAndCreate(long totalHit, HouseIndexTemplate template) {
        DeleteByQueryRequestBuilder builer = DeleteByQueryAction.INSTANCE.newRequestBuilder(client);
        builer.filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, template.getHouseId()));
        builer.source(INDEX_NAME);

        logger.debug("Delete by query for house: " + builer);

        BulkByScrollResponse response = builer.get();

        long deleted = response.getDeleted();
        if (deleted != totalHit) {
            logger.warn("Need delete {}, but {} was deleted!", totalHit, deleted);
            return false;
        } else {
            return create(template);
        }



    }

    /*@Override
    public void remove(Long houseId) {
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(client);
        builder.filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        builder.source(INDEX_NAME);

        logger.debug("Debug by query for house: " + builder);

        BulkByScrollResponse response = builder.get();
        long deleted = response.getDeleted();
        logger.debug("Delete total " + deleted);
    }*/

    /**
     * 房源搜索引擎方法
     * @param rentSearch 查询条件
     * @return 搜索到的房源·的ids
     */
    @Override
    public ServiceMultiResult<Long> query(RentSearch rentSearch) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //query
        //城市一定有
        boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, rentSearch.getCityEnName()));
        //区可能有也可能没有
        if (rentSearch.getRegionEnName() != null && !"*".equals(rentSearch.getRegionEnName())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, rentSearch.getRegionEnName()));
        }

        //关键词搜索
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(
                rentSearch.getKeywords(),
                HouseIndexKey.TITLE,
                HouseIndexKey.TRAFFIC,
                HouseIndexKey.DISTRICT,
                HouseIndexKey.ROUND_SERVICE,
                HouseIndexKey.SUBWAY_LINE_NAME,
                HouseIndexKey.SUBWAY_STATION_NAME
                );
        boolQueryBuilder.must(multiMatchQueryBuilder);

        //面积
        RentValueBlock areaBlock = RentValueBlock.matchArea(rentSearch.getAreaBlock());
        if (!RentValueBlock.ALL.equals(areaBlock)) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.AREA);
            if (areaBlock.getMin() > 0) {
                rangeQueryBuilder.gte(areaBlock.getMin());
            }
            if (areaBlock.getMax() > 0) {
                rangeQueryBuilder.lte(areaBlock.getMax());
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        //价格
        RentValueBlock priceBlock = RentValueBlock.matchPrice(rentSearch.getPriceBlock());
        if (!RentValueBlock.ALL.equals(priceBlock)) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.PRICE);
            if (priceBlock.getMin() > 0) {
                rangeQueryBuilder.gte(priceBlock.getMin());
            }
            if (priceBlock.getMax() > 0) {
                rangeQueryBuilder.lte(priceBlock.getMax());
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        //房源朝向，出租方式
        if (rentSearch.getDirection() > 0) {
            TermQueryBuilder termQueryBuilder =
                    QueryBuilders.termQuery(HouseIndexKey.DIRECTION, rentSearch.getDirection());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        if (rentSearch.getRentWay() > -1) {
            TermQueryBuilder termQueryBuilder =
                    QueryBuilders.termQuery(HouseIndexKey.RENT_WAY, rentSearch.getRentWay());
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //request
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(INDEX_NAME);
        searchRequestBuilder.setTypes(INDEX_TYPE);
        searchRequestBuilder.setQuery(boolQueryBuilder);
        searchRequestBuilder.addSort(
                HouseSort.getSortKey(rentSearch.getOrderBy()), SortOrder.fromString(rentSearch.getOrderDirection()));
        searchRequestBuilder.setFrom(rentSearch.getStart());
        searchRequestBuilder.setSize(rentSearch.getSize());


        logger.debug(searchRequestBuilder.toString());


        //向Es发送搜索请求，并判断请求的响应
        List<Long> houseIds = new ArrayList<>();
        SearchResponse searchResponse = searchRequestBuilder.get();
        if (searchResponse.status() != RestStatus.OK) {
            //在这里可以发邮件，发短信，等等的通知操作，这里只是简单用了log的debug处理了
            logger.warn("Search status is not ok for " + searchRequestBuilder);
            return new ServiceMultiResult<>(0, houseIds);
        }

        //获取到有查询到的house的id
        SearchHits hits = searchResponse.getHits();
        hits.forEach(hit -> houseIds.add(Longs.tryParse(String.valueOf(hit.getSource().get(HouseIndexKey.HOUSE_ID)))));

        //封装服务返回对象，返回
        return new ServiceMultiResult<>(searchResponse.getHits().totalHits, houseIds);
    }


}
