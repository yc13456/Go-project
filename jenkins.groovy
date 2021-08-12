node {
    stage("cheakout"){
        checkout([$class: 'GitSCM', branches: [[name: '**']], extensions: [], userRemoteConfigs: [[credentialsId: 'e41f358d-3d20-4c4e-99cb-8726e11819a2', url: 'git@github.com:yc13456/Go-project.git']]])
    }
    stage("build Images"){
        echo "build Images start。。。"
	docker buildx build --platform linux/arm64 -f Dockerfile -t 2267024990/hello . --push
	echo "build images success"
    }
    stage("resultds"){
        echo "result"
	echo "result"
    }
}
