var map = document.getElementById('map');
var colorPicker = document.getElementById('color-picker');
var ctx = map.getContext('2d');

var canvasWidth= map.width = 1000;
var canvasHeight = map.height = 1000;


function hexToRgb(hex) {
  var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result? new ColorHandler(parseInt(result[1],16), parseInt(result[2],16), parseInt(result[3],16), 255): null;
}


var labelPosX = document.getElementById("xCord"), labelPosY = document.getElementById("yCord");

var mapSizeX = 100;
var mapSizeY = 100;
var offscreenCanvas = new OffscreenCanvas(mapSizeX, mapSizeY);
var ctxBack = offscreenCanvas.getContext('2d');

var mousePointX = 0;
var mousePointY = 0;

var image = ctx.createImageData(mapSizeX,mapSizeY);

function calculateMousePointAt(event){
  return [
      Math.ceil((parseInt(event.clientX)-map.offsetLeft)/zoom)-1,
      Math.ceil((parseInt(event.clientY+window.scrollY))/zoom)-1,
      parseInt(event.clientX),
      parseInt(event.clientY)
    ];
}

class ColorHandler {
  constructor(red, green, blue){
    this.red = red;
    this.green = green;
    this.blue = blue;
  }

  pack(){
    return this.red << 16 | this.green << 8 | this.blue;
  }

  unpack(colorNumber){
    this.red = (colorNumber >> 16) & 0xff;
    this.green = (colorNumber >> 8) & 0xff;
    this.blue = colorNumber & 0xff;
    return this;
  }
}

function writePixel(x, y, color){
  image.data[y*mapSizeY*4+x*4] = color.red;
  image.data[y*mapSizeY*4+x*4+1] = color.green;
  image.data[y*mapSizeY*4+x*4+2] = color.blue;
  image.data[y*mapSizeY*4+x*4+3] = 255;
}

function postAjax(url, data, callback){
  var xhr = new XMLHttpRequest();
  xhr.open('POST', url, true);
  xhr.setRequestHeader('Content-Type', 'application/json');
  xhr.onreadystatechange = function() {
    if (xhr.readyState == 4 && xhr.status == 200) {
      callback(JSON.parse(xhr.responseText));
    }
  };
  xhr.send(JSON.stringify(data));
}

function getAjax(url, callback){
  var xhr = new XMLHttpRequest();
  xhr.open('GET', url, true);
  xhr.onreadystatechange = function() {
    if (xhr.readyState == 4 && xhr.status == 200) {
      callback(JSON.parse(xhr.responseText));
    }
  };
  xhr.send();
}

function updateState(res){
  if(res.state == "ERROR"){
    alert(res.errorMessage);
    return false;
  }
  if(res.state == "PIXEL"){
    var px = res.pixel;
    writePixel(px.x, px.y, new ColorHandler().unpack(px.color));
  }
  if(res.state == "UPDATE"){
    for(var y = 0; y < res.map.height; y++) {
      for(var x = 0; x < res.map.width; x++) {
        var pox = res.map.colors[y*res.map.width+x]
        writePixel(x,y, new ColorHandler().unpack(pox));
      }
    }
  }

  return true;
}

function sendPixel(x,y, color){
  postAjax('http://localhost:8080/drawPixel', {x: x, y: y, color: color.pack()}, function(response){
    updateState(response);
  });
}

var mouseDown = false;
map.addEventListener('mousedown', function(e){
  var mousePoint = calculateMousePointAt(e);
  mouseX=mousePoint[0];
  mouseY=mousePoint[1];
  mouseDown = true;
  sendPixel(mouseX,mouseY, hexToRgb(colorPicker.value));
});
map.addEventListener('mouseup', function(e){mouseDown = false;});
map.addEventListener('mousemove', function(e){
  var mousePoint = calculateMousePointAt(e);
  labelPosX.innerHTML = mousePoint[0];
  labelPosY.innerHTML = mousePoint[1];
  mousePointX = mousePoint[0];
  mousePointY = mousePoint[1];
});

var zoom = 10;

function animationLoop(){
  ctx.fillStyle = "rgba(0,0,0,1)";
  ctx.fillRect(0,0, canvasWidth, canvasHeight);
  ctx.imageSmoothingEnabled = false;
  ctxBack.putImageData(image,0,0);

  ctx.drawImage(offscreenCanvas, 0,0 , mapSizeX, mapSizeY, 0,0, mapSizeX*zoom, mapSizeY*zoom);

  ctx.strokeStyle = "rgb(10,232,201,100)";
  ctx.strokeRect(mousePointX*zoom, mousePointY*zoom, 10,10);

  window.requestAnimationFrame(animationLoop);
}
animationLoop();


function loadState(){
  getAjax('http://localhost:8080/load', function(response){
    updateState(response);
  })
}

var updateTimer = setInterval(loadState, 1000);
loadState();