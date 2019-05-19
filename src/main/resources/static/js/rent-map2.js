/**
 * @author wangjiajia
 * @create 2019-5-18 21:25:37
 *
 */

//定义一个地区数据的对象
var regionCountMap = {};
//定义一个数组表示标签列表
var labels = [];


//初始化加载百度地图，把百度地图在地图找房页面的allMap div里面显示出来
function load(city, regions, aggData) {

    // 百度地图API功能
    /*var map = new BMap.Map("allmap");
    var point = new BMap.Point(116.417854,39.921988);
    map.centerAndZoom(point, 15);
    var opts = {
        position : point,    // 指定文本标注所在的地理位置
        offset   : new BMap.Size(30, -30)    //设置文本偏移量
    }
    var label = new BMap.Label("欢迎使用百度地图，这是一个简单的文本标注哦~", opts);  // 创建文本标注对象
    label.setStyle({
        color : "red",
        fontSize : "12px",
        height : "20px",
        lineHeight : "20px",
        fontFamily:"微软雅黑"
    });
    map.addOverlay(label);*/


    //这里是百度地图JavaScript API的功能

    //创建一个百度地图实例，minZoom设置地图显示最大级别为12个城市
    var map = new BMap.Map("allmap", {minZoom: 12});
    //创建一个表示城市的中心位置的点，从参数city中取出城市中心位置的经纬度
    //var point = new BMap.Point(city.baiduMapLongitude, city.baiduMapLatitude);
    var point = new BMap.Point(116.403119,39.914935); //北京天安门坐标
    //var point = new BMap.Point(116.403119,39.914935); //广州坐标
    //初始化百度地图，设置中心点和显示级别
    map.centerAndZoom(point, 12);


    map.addControl(new BMap.NavigationControl({enableGeolocation: true}));//给地图添加比例尺控件
    map.addControl(new BMap.ScaleControl({anchor: BMAP_ANCHOR_TOP_LEFT}));//左上角？
    map.enableScrollWheelZoom(true);//打开鼠标滚轮缩放地图的功能

    //把aggData中的数据放到regionCountMap中
    for (var i = 0; i < aggData.length; i++) {
        regionCountMap[aggData[i].key] = aggData[i].count;
    }


    drawRegion(map, regions);

}

/**
 * 把各个region的聚合数据放在地图上
 * @param map
 * @param regionList
 */
function drawRegion(map, regionList) {

    //多边形覆盖物
    var polygonContext = {};

    for (var i = 0; i < regionList.length; i++) {
        var regionPoint;
        var textLabel;
        //边界
        var boundary = new BMap.Boundary();



        //regionPoint = new BMap.Point(regionList[i].baiduMapLongtitude, regionList[i].baiduMapLatitude);
        //准备一个地图上点的位置
        regionPoint = new BMap.Point(regionList[i].baiduMapLongitude, regionList[i].baiduMapLatitude);

        //console.log(regionList[i].baiduMapLongitude);
        //console.log(regionList[i].baiduMapLatitude);

        //根据region从regionCountMap中获取对应的聚合数据
        var houseCount = 0;
        if (regionList[i].en_name in regionCountMap) {
            houseCount = regionCountMap[regionList[i].en_name];
        }

        //准备标签的内容, 并把聚合的数据放进去
        var textContent =
            '<p style="margin-top: 20px; pointer-events: none">'+regionList[i].cn_name + '</p>' +
            '<p style="pointer-events: none">'+ houseCount +'套</p>'

        //设置标签的内容和放在地图上的位置
        textLabel = new BMap.Label(textContent, {
            position: regionPoint, //标签放在地图上的哪个位置
            offset: new BMap.Size(-40, 20) //文本偏移量
        });

        //设置标签的样式
        textLabel.setStyle({
            height: '78px',
            width: '78px',
            color: '#fff',
            backgroundColor: '#0054a5',
            border: '0px solid rgb(255, 0, 0)',
            borderRadius: '50%', //它会让textLabel变成一个圆
            fontWeight: 'bold',
            display: 'inline',
            lineHeight: 'normal',
            textAlign: 'center',
            opacity: '0.8',
            zIndex: 2,
            overflow: 'hidden'
        });

        //把标签放到地图上
        map.addOverlay(textLabel);

        //把每个标签都放进事先定义好的labels中
        labels.push(textLabel);





        /*记录行政区域的覆盖物*/
        //点集合
        polygonContext[textContent] = [];
        //闭包传参
        (function (textContent) {
            //获取行政区域，参数里的city是哪里来的啊
            boundary.get(city.cn_name + regionList[i].cn_name, function (rs) {

                //console.log(regionList[i].cn_name);//报错Cannot read property 'cn_name' of undefined
                //行政区域 点集合 的长度
                var count = rs.boundaries.length;
                if (count === 0) {
                    alert("未能获取当前输入的行政区域");
                    return;
                }

                //建立多边形覆盖物
                for (var j = 0; j < count; j++) {
                    var polygon = new BMap.Polygon(
                        rs.boundaries[j],
                        {
                            strokeWeight: 2,
                            strokeColor: '#0054a5',
                            fillOpacity: 0.3,
                            fillColor: '0054a5'
                        }
                    );
                    //添加覆盖物
                    map.addOverlay(polygon);

                    //把polygon放到事先定义好的polygonContext里面
                    polygonContext[textContent].push(polygon);

                    //初始化的时候先把区域上的覆盖物隐藏
                    polygon.hide();
                }
            })
        })(textContent);

        //给给每个区域的label添加鼠标事件，当鼠标移动到label上的时候，显示出这个区域的覆盖物
        textLabel.addEventListener('mouseover', function (event) {
            var label = event.target;
            //从label中获取点集合
            var boundaries = polygonContext[label.getContent()];

            //给label设置一个新的样式，把它的背景色设成xx色
            label.setStyle({
                backgroundColor: '#1AA591'
            });

            for (var k = 0; k < boundaries.length; k++) {
                boundaries[k].show();
            }
        });

        //给每个区域的label添加鼠标事件，当鼠标移出label的时候，隐藏这个区域的覆盖物
        textLabel.addEventListener("mouseout", function (event) {
            var label = event.target;
            var boundaries = polygonContext[label.getContent()];

            //把label的样式设置初始的颜色，蓝色
            label.setStyle({
                backgroundColor: '#0054a5'
            });

            for (var l = 0; l < boundaries.length; l++) {
                boundaries[l].hide();
            }
        });

        textLabel.addEventListener('click', function (event) {
            var label = event.target;
            var map = label.getMap();
            map.zoomIn();
            map.panTo(event.point);
        })







    }
}