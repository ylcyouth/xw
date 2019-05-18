/**
 * @author wangjiajia
 * @create 2019-5-18 21:25:37
 *
 */

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

    drawRegion(map, regions);

}

/**
 * 把各个region的聚合数据放在地图上
 * @param map
 * @param regionList
 */
function drawRegion(map, regionList) {

    for (var i = 0; i < regionList.length; i++) {
        var regionPoint;
        var textLabel;

        //regionPoint = new BMap.Point(regionList[i].baiduMapLongtitude, regionList[i].baiduMapLatitude);
        regionPoint = new BMap.Point(regionList[i].baiduMapLongitude, regionList[i].baiduMapLatitude);

        console.log(regionList[i].baiduMapLongitude);
        console.log(regionList[i].baiduMapLatitude);

        var textContent =
            '<p style="margin-top: 20px; pointer-events: none">'+regionList[i].cn_name + '</p>' +
            '<p style="pointer-events: none">'+ 0 +'套</p>'

        textLabel = new BMap.Label(textContent, {
            position: regionPoint, //标签放在地图上的哪个位置
            offset: new BMap.Size(-40, 20) //文本偏移量
        });

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
    }
}