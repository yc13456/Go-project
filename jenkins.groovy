node {

    stage("build Images"){
	docker buildx build --platform linux/arm64 -f Dockerfile -t 2267024990/hello . --push
	echo "build images success"
    }
    stage("resultds"){
        echo "result"
	echo "result"
    }
}
