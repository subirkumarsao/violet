<html>
<head>
<script src="https://code.jquery.com/jquery-3.4.1.min.js"
	integrity="sha256-CSXorXvZcTkaix6Yvo6HppcZGetbYMGWSFlBw8HfCJo="
	crossorigin="anonymous"></script>
</head>
<body>

	<input type="file" name="file" id="file" />
	<input type="hidden" name="base64Image" id="fileData" />
	<button id="but_upload" onclick="recognizeFace()">Process</button>
	<div id="imageDiv" style="overflow:auto;">
	</div>
	
	<div id="video">
		<video id="myVideo">
		  <!--  <source id="mp4_src" src="videos/video2.mp4" type="video/mp4">
		  Your browser does not support the video tag.  -->
		</video>	
	</div>
	<canvas id="canvas" style="overflow:auto; display: none;"></canvas>
	
	<button onclick="playVid()" type="button">Play Video</button>
	<button onclick="pauseVid()" type="button">Pause Video</button>
	<input type="text" name="label" id="label" />
	<button onclick="capture(trainFace)" type="button">Train</button>
	<button onclick="capture(recognizeFace)" type="button">Recognize</button>  
	
	<script>
	var x = document.getElementById("myVideo"); 
	
	function playVid() { 
	  x.play();
	} 
	
	function pauseVid() { 
	  x.pause(); 
	} 
	
	// Get access to the camera!
	if(navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
	    // Not adding `{ audio: true }` since we only want video now
	    navigator.mediaDevices.getUserMedia({ video: true }).then(function(stream) {
	        //video.src = window.URL.createObjectURL(stream);
	        x.srcObject = stream;
	    });
	}
	
	function capture(callback) {
	    var canvas = document.getElementById('canvas');     
	    var video = document.getElementById('myVideo');
	    canvas.width = video.videoWidth;
	    canvas.height = video.videoHeight;
	    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);  
	    console.log(canvas.width, canvas.height);
	    canvas.toBlob((blob) => {
	    	var reader = new FileReader();
	    	 reader.readAsDataURL(blob); 
	    	 reader.onloadend = function() {
	    	     base64data = reader.result;   
	    	     base64data = base64data.replace('data:image/png;base64,','');
	    	     $("#fileData").val(base64data);
	    	     callback();
	    	 }
	    });
	}
	</script>
</body>
<script type="text/javascript">
	function recognizeFace(){
		var fd = new FormData();
		var files = $('#file')[0].files[0];
		var base64Image = $("#fileData").val();
		fd.append('file', files);
		fd.append('base64Image', base64Image);

		$.ajax({
			url : 'face/recognize',
			type : 'POST',
			data : fd,
			enctype : 'multipart/form-data',
			contentType : false,
			processData : false,
			success : function(response) {
				$("#imageDiv").empty();
				//$("#imageDiv").append("<img src='images/"+response.fileName+"' style='position:absolute;'/>");
				for(var index in response){
					var rect = response[index].face;
					$("#imageDiv").append("<svg x='"+rect.x+"' y='"+rect.y+"' width='"+rect.width+"' height='"+rect.height
							+"' style='position:absolute;left: "+rect.x+"px;top: "+rect.y
							+"px;'><rect width='100%' height='100%' style='stroke: RED; stroke-width: 10px; fill: none;' />"
							+ "<text x='20' y='20' font-family='Verdana' font-size='20' fill='yellow'>"+response[index].label+"</text>"
							+"</svg>");
				}
				var video = document.getElementById('myVideo');
				if(!video.paused) {
					capture(recognizeFace);
	    	    }
			},
		});
	}
	
	function trainFace(){
		var fd = new FormData();
		var files = $('#file')[0].files[0];
		var base64Image = $("#fileData").val();
		fd.append('file', files);
		fd.append('base64Image', base64Image);
		fd.append('label', $("#label").val());

		$.ajax({
			url : 'face/train',
			type : 'POST',
			data : fd,
			enctype : 'multipart/form-data',
			contentType : false,
			processData : false,
			success : function(response) {
				$("#imageDiv").empty();
				console.log(response);
				var video = document.getElementById('myVideo');
				if(!video.paused) {
					capture(trainFace);
	    	    }
			},
		});
	}
</script>
</html>
