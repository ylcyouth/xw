/**
 * @author wangjiajia
 * @create 2019-5-18 21:25:37
 *
 */

//初始化加载百度地图，把百度地图在地图找房页面的allMap div里面显示出来
function load(city, regions, aggData) {

    //这里是百度地图JavaScript API的功能

    //创建一个百度地图实例，minZoom设置地图显示最大级别为12个城市
    var map = new BMap.Map("allmap", {minZoom: 12});
    //创建一个表示城市的中心位置的点，从参数city中取出城市中心位置的经纬度
    //var point = new BMap.Point(city.baiduMapLongitude, city.baiduMapLatitude);
    //var point = new BMap.Point(116.403119,39.914935); //北京天安门坐标
    var point = new BMap.Point(116.403119,39.914935); //广州坐标
    //初始化百度地图，设置中心点和显示级别
    map.centerAndZoom(point, 12);

    //给地图添加比例尺控件
    map.addControl(new BMap.NavigationControl({enableGeolocation: true}));
    //左上角？
    map.addControl(new BMap.ScaleControl({anchor: BMAP_ANCHOR_TOP_LEFT}));
    //打开鼠标滚轮缩放地图的功能
    map.enableScrollWheelZoom(true);

    /*for (var i = 0; i < aggData.length; i++) {


    }*/









}