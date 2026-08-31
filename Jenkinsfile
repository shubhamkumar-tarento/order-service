// Runs the same on Linux and Windows agents.
def run(String command) {
	if (isUnix()) {
		sh command
	} else {
		bat command
	}
}

pipeline {
	agent any

	options {
		timestamps()
		buildDiscarder(logRotator(numToKeepStr: '10'))
		timeout(time: 30, unit: 'MINUTES')
	}

	parameters {
		string(name: 'K8S_NAMESPACE', defaultValue: 'default', description: 'Namespace to deploy into')
		booleanParam(name: 'PUSH_IMAGE', defaultValue: false, description: 'Push to the registry (leave off for a local cluster)')
	}

	environment {
		// e.g. docker.io/yourusername - leave blank to build a local-only image.
		REGISTRY    = ''
		IMAGE_NAME  = 'order-service'
		IMAGE_TAG   = "${env.BUILD_NUMBER}"
		IMAGE       = "${env.REGISTRY ? env.REGISTRY + '/' : ''}${env.IMAGE_NAME}:${env.BUILD_NUMBER}"
	}

	stages {
		stage('Checkout') {
			steps {
				checkout scm
			}
		}

		stage('Build & Test') {
			steps {
				run 'mvn -B clean verify'
			}
			post {
				always {
					junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
					archiveArtifacts artifacts: 'target/order-service.jar', fingerprint: true, allowEmptyArchive: true
				}
			}
		}

		stage('Docker Build') {
			steps {
				run "docker build -t ${IMAGE} -t ${env.REGISTRY ? env.REGISTRY + '/' : ''}${IMAGE_NAME}:latest ."
			}
		}

		stage('Docker Push') {
			when {
				expression { return params.PUSH_IMAGE && env.REGISTRY?.trim() }
			}
			steps {
				withCredentials([usernamePassword(credentialsId: 'docker-registry-credentials',
						usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
					run "docker login ${REGISTRY} -u ${DOCKER_USER} -p ${DOCKER_PASS}"
				}
				run "docker push ${IMAGE}"
				run "docker push ${env.REGISTRY ? env.REGISTRY + '/' : ''}${IMAGE_NAME}:latest"
			}
		}

		stage('Deploy to Kubernetes') {
			steps {
				// 'kubeconfig-credentials' is a Secret file credential holding a kubeconfig.
				withCredentials([file(credentialsId: 'kubeconfig-credentials', variable: 'KUBECONFIG')]) {
					run "kubectl -n ${params.K8S_NAMESPACE} apply -f k8s/deployment.yaml -f k8s/service.yaml"
					run "kubectl -n ${params.K8S_NAMESPACE} set image deployment/order-service order-service=${IMAGE} --record=false"
					run "kubectl -n ${params.K8S_NAMESPACE} rollout status deployment/order-service --timeout=180s"
				}
			}
		}

		stage('Smoke Test') {
			steps {
				withCredentials([file(credentialsId: 'kubeconfig-credentials', variable: 'KUBECONFIG')]) {
					// Hit the service from inside the cluster so no Ingress is needed.
					run "kubectl -n ${params.K8S_NAMESPACE} run smoke-${BUILD_NUMBER} --rm -i --restart=Never --image=curlimages/curl:8.11.1 -- curl -sf http://order-service:8080/actuator/health"
				}
			}
		}
	}

	post {
		success {
			echo "Deployed ${IMAGE} to namespace ${params.K8S_NAMESPACE}"
		}
		failure {
			echo "Build ${env.BUILD_NUMBER} failed - check the stage log above."
		}
		always {
			cleanWs()
		}
	}
}
