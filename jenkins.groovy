node {
    stage("cheakout"){
        checkout([$class: 'GitSCM', branches: [[name: '**']], extensions: [], userRemoteConfigs: [[credentialsId: 'e41f358d-3d20-4c4e-99cb-8726e11819a2', url: 'git@github.com:yc13456/Go-project.git']]])
    }
    stage("test build"){
        echo "test build。。。"
	echo "test build。。。"
	echo "test build。。。"
    }
    stage("resultds"){
        echo "result"
	echo "result"
    }
}