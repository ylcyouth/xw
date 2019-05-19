package com.gagaco.xunxuproj2.service.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gagaco.xunxuproj2.base.HouseSort;
import com.gagaco.xunxuproj2.base.RentValueBlock;
import com.gagaco.xunxuproj2.entity.House;
import com.gagaco.xunxuproj2.entity.HouseDetail;
import com.gagaco.xunxuproj2.entity.HouseTag;
import com.gagaco.xunxuproj2.entity.SupportAddress;
import com.gagaco.xunxuproj2.repository.HouseDetailRepository;
import com.gagaco.xunxuproj2.repository.HouseRepository;
import com.gagaco.xunxuproj2.repository.HouseTagRepository;
import com.gagaco.xunxuproj2.repository.SupportAddressRepository;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.supportaddress.ISupportAddressService;
import com.gagaco.xunxuproj2.web.form.RentSearch;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkIndexByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private ISupportAddressService supportAddressService;

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

        //获取百度地图经纬度, 获取成功后设置到houseTemplate中
        SupportAddress city = supportAddressRepository
                .findByEnNameAndLevel(house.getCityEnName(), SupportAddress.Level.CITY.getValue());
        SupportAddress region = supportAddressRepository
                .findByEnNameAndLevel(house.getRegionEnName(), SupportAddress.Level.REGION.getValue());

        String address = city.getCnName()
                + region.getCnName()
                + house.getStreet()
                + house.getDistrict()
                + detail.getDetailAddress();

        ServiceResult<BaiduMapLocation> baiduMapLocation = supportAddressService
                .getBaiduMapLocation(city.getCnName(), address);

        if (!baiduMapLocation.isSuccess()) {
            this.index(message.getHouseId(), message.getRetry() + 1);
            return;
        }

        template.setLocation(baiduMapLocation.getResult());


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

    //向kafka发送索引房源的消息
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

        BulkIndexByScrollResponse bulkIndexByScrollResponse = builder.get();
        long deleted = bulkIndexByScrollResponse.getDeleted();
        logger.debug("Delete total " + deleted);
        if (deleted <= 0) {
            this.remove(houseId, message.getRetry() + 1);
        }

        //5.6的写法
        //BulkByScrollResponse response = builder.get();
        //long deleted = response.getDeleted();
//        logger.debug("Delete total " + deleted);
//
//        if (deleted <= 0) {
//            this.remove(houseId, message.getRetry() + 1);
//        }
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

        //存houseIndexTemplate之前，先更新和索引绑定的suggest
        if (!updateSuggest(template)) {
            return false;
        }

        try {
            IndexRequestBuilder indexRequestBuilder = client.prepareIndex(INDEX_NAME, INDEX_TYPE);
            //indexRequestBuilder.setSource(objectMapper.writeValueAsBytes(template), XContentType.JSON);
            indexRequestBuilder.setSource(objectMapper.writeValueAsBytes(template));
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

        //修改es中houseTemplate文档之前先修改要存入houseIndexTemplate中的suggest
        if (!updateSuggest(template)) {
            return false;
        }

        try {
            UpdateRequestBuilder updateRequestBuilder = client.prepareUpdate(INDEX_NAME, INDEX_TYPE, id);
            //updateRequestBuilder.setDoc(objectMapper.writeValueAsBytes(template), XContentType.JSON);
            updateRequestBuilder.setDoc(objectMapper.writeValueAsBytes(template));
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

        BulkIndexByScrollResponse response = builer.get();

        long deleted = response.getDeleted();
        if (deleted != totalHit) {
            logger.warn("Need delete {}, but {} was deleted!", totalHit, deleted);
            return false;
        } else {
            return create(template);
        }

        /*BulkByScrollResponse response = builer.get();

        long deleted = response.getDeleted();
        if (deleted != totalHit) {
            logger.warn("Need delete {}, but {} was deleted!", totalHit, deleted);
            return false;
        } else {
            return create(template);
        }*/



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

        boolQueryBuilder.should(
                QueryBuilders.matchQuery(HouseIndexKey.TITLE, rentSearch.getKeywords())
                        .boost(2.0F)
        );

        boolQueryBuilder.should(
                QueryBuilders.multiMatchQuery(
                        rentSearch.getKeywords(),
                        HouseIndexKey.TRAFFIC,
                        HouseIndexKey.DISTRICT,
                        HouseIndexKey.ROUND_SERVICE,
                        HouseIndexKey.SUBWAY_LINE_NAME,
                        HouseIndexKey.SUBWAY_STATION_NAME
                )
        );

        //request
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(INDEX_NAME);
        searchRequestBuilder.setTypes(INDEX_TYPE);
        searchRequestBuilder.setQuery(boolQueryBuilder);
        //searchRequestBuilder.addSort(
        //        HouseSort.getSortKey(rentSearch.getOrderBy()), SortOrder.fromString(rentSearch.getOrderDirection()));
        searchRequestBuilder.setFrom(rentSearch.getStart());
        searchRequestBuilder.setSize(rentSearch.getSize());

        searchRequestBuilder.setFetchSource(new String[]{HouseIndexKey.HOUSE_ID, HouseIndexKey.TITLE}, null);


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
        //return new ServiceMultiResult<>(searchResponse.getHits().totalHits, houseIds);//5.6写法
        return new ServiceMultiResult<>(searchResponse.getHits().totalHits(), houseIds);//5.2的写法
    }

    /**
     * 搜索补全的关键词
     *
     * @param prefix
     * @return
     */
    @Override
    public ServiceResult<List<String>> suggest(String prefix) {

        CompletionSuggestionBuilder suggestion = SuggestBuilders.completionSuggestion("suggest");
        suggestion.prefix(prefix);
        suggestion.size(5);

        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion("autocomplete", suggestion);

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(INDEX_NAME);
        searchRequestBuilder.setTypes(INDEX_TYPE);
        searchRequestBuilder.suggest(suggestBuilder);

        logger.debug(searchRequestBuilder.toString());

        SearchResponse searchResponse = searchRequestBuilder.get();

        Suggest suggest = searchResponse.getSuggest();

        if (suggest == null) {
            return ServiceResult.of(new ArrayList<>());
        }

        /*Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>>
                autocomplete = suggest.getSuggestion("autocomplete");*/
        Suggest.Suggestion result = suggest.getSuggestion("autocomplete");

        //表示候选词的数量
        int maxSuggest = 0;

        Set<String> suggestSet = new HashSet<>();

        for (Object term : result.getEntries()) {
            if (term instanceof CompletionSuggestion.Entry) {
                CompletionSuggestion.Entry item = (CompletionSuggestion.Entry) term;

                //去空
                if (item.getOptions().isEmpty()) {
                    continue;
                }

                for (CompletionSuggestion.Entry.Option option : item.getOptions()) {
                    String tip = option.getText().string();

                    //去重
                    if (suggestSet.contains(tip)) {
                        continue;
                    }

                    suggestSet.add(tip);
                    maxSuggest++;
                }
            }

            //只拿出来5个候选词
            if (maxSuggest > 5) {
                break;
            }
        }

        List<String> suggests = Lists.newArrayList(suggestSet.toArray(new String[]{}));
        return ServiceResult.of(suggests);
    }

    /**
     * 聚合小区的房源数量
     *
     * @param cityName
     * @param regionEnName
     * @param district
     */
    @Override
    public ServiceResult<Long> aggregateDistrictHouse(String cityName, String regionEnName, String district) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityName))
                .filter(QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, regionEnName))
                .filter(QueryBuilders.termQuery(HouseIndexKey.DISTRICT, district));

        TermsAggregationBuilder aggBuilder = AggregationBuilders.terms(HouseIndexKey.AGG_DISTRICT).
                        field(HouseIndexKey.DISTRICT);

        SearchRequestBuilder requestBuilder = client.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQueryBuilder)
                .addAggregation(aggBuilder)
                .setSize(0);

        logger.debug(requestBuilder.toString());

        SearchResponse searchResponse = requestBuilder.get();

        if (searchResponse.status() == RestStatus.OK) {
            //Aggregation aggregation = searchResponse.getAggregations().get(HouseIndexKey.AGG_DISTRICT);
            //注意用Terms来接收 terms extends MultiBucketsAggregation extends Aggregation
            Terms terms = searchResponse.getAggregations().get(HouseIndexKey.AGG_DISTRICT);
            if (terms.getBuckets() != null && !terms.getBuckets().isEmpty()) {
                return ServiceResult.of(terms.getBucketByKey(district).getDocCount());
            } else {
                logger.warn("Failed to Aggregate for " + HouseIndexKey.AGG_DISTRICT);
            }
        }
        return ServiceResult.of(0L);
    }

    /**
     * 聚合城市房源数据
     *
     * @param cityName
     */
    @Override
    public ServiceMultiResult<HouseBucketDto> aggregateMap(String cityName) {

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        boolQuery.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityName));

        TermsAggregationBuilder agg = AggregationBuilders.terms(HouseIndexKey.AGG_REGION)
                .field(HouseIndexKey.REGION_EN_NAME);

        SearchRequestBuilder request = client.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQuery)
                .addAggregation(agg);

        SearchResponse response = request.get();

        logger.debug(request.toString());

        List<HouseBucketDto> bucketDtos = new ArrayList<>();

        if (response.status() != RestStatus.OK) {
            logger.warn("Aggregation status is not ok for " +  request);
            return new ServiceMultiResult<>(0, bucketDtos);
        }

        Terms term = response.getAggregations().get(HouseIndexKey.AGG_REGION);
        for (Terms.Bucket bucket : term.getBuckets()) {
            bucketDtos.add(new HouseBucketDto(bucket.getKeyAsString(), bucket.getDocCount()));
        }

        return new ServiceMultiResult<>(response.getHits().totalHits(), bucketDtos);
    }

    /**
     * 城市级别查询
     * 查询一个城市下面所有的房源id
     * @param cityEnName
     * @param orderBy
     * @param orderDirection
     * @param start
     * @param size
     */
    @Override
    public ServiceMultiResult<Long> mapQuery(String cityEnName, String orderBy, String orderDirection, int start, int size) {

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityEnName));

        SearchRequestBuilder searchRequest = client.prepareSearch(INDEX_NAME);
        searchRequest.setTypes(INDEX_TYPE)
                .setQuery(boolQuery)
                .addSort(HouseSort.getSortKey(orderBy), SortOrder.fromString(orderDirection))
                .setFrom(start)
                .setSize(size);

        List<Long> houseIds = new ArrayList<>();
        SearchResponse searchResponse = searchRequest.get();
        if (searchResponse.status() != RestStatus.OK) {
            logger.warn("Search status is not ok for " + searchRequest);
            return new ServiceMultiResult<>(0, houseIds);
        }

        for (SearchHit hit : searchResponse.getHits()) {
            houseIds.add(Longs.tryParse(String.valueOf(hit.getSource().get(HouseIndexKey.HOUSE_ID))));
        }

        return new ServiceMultiResult<>(searchResponse.getHits().getTotalHits(), houseIds);
    }

    private boolean updateSuggest(HouseIndexTemplate indexTemplate) {

        AnalyzeRequestBuilder analyzeRequestBuilder = new AnalyzeRequestBuilder(
                client,
                AnalyzeAction.INSTANCE,
                INDEX_NAME,
                indexTemplate.getTitle(),
                //indexTemplate.getLayoutDesc(),
                indexTemplate.getRoundService(),
                indexTemplate.getDescription(),
                indexTemplate.getSubwayLineName(),
                indexTemplate.getSubwayStationName()
        );

        analyzeRequestBuilder.setAnalyzer("ik_smart");

        logger.debug(analyzeRequestBuilder.toString());

        AnalyzeResponse analyzeResponse = analyzeRequestBuilder.get();

        List<AnalyzeResponse.AnalyzeToken> tokens = analyzeResponse.getTokens();

        if (tokens == null) {
            logger.warn("Can not analyze token for house: " + indexTemplate.getHouseId());
            return false;
        }

        List<HouseSuggest> suggestList = new ArrayList<>();

        for (AnalyzeResponse.AnalyzeToken analyzeToken : tokens) {

            //去除数字和小于2个字的分词结果
            if ("<NUM>".equals(analyzeToken.getType()) || analyzeToken.getTerm().length() < 2) {
                continue;
            }

            HouseSuggest houseSuggest = new HouseSuggest();
            houseSuggest.setInput(analyzeToken.getTerm());
            suggestList.add(houseSuggest);
        }

        //定制化小区自动补全
        HouseSuggest houseSuggest = new HouseSuggest();
        houseSuggest.setInput(indexTemplate.getDistrict());
        suggestList.add(houseSuggest);

        indexTemplate.setSuggest(suggestList);
        return true;

    }


}
