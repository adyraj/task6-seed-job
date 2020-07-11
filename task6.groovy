job("job1"){
             	
              scm {
                               github('adyraj/phpweb', 'master')
                       }
              triggers {
                                  upstream('seed', 'SUCCESS')
                            }
              steps{
                             shell(''' if ls / | grep myweb 
                                         then
                                         cp -rvf * /myweb
                                         else
                                         mkdir /myweb
                                         cp -rvf * /myweb
                                         fi''')
                   }
}


job('job2'){
                   
	 triggers {
                                  upstream('job1', 'SUCCESS')
                            }
                   steps{
                            shell(''' if ls /myweb/ | grep .php
                                      then
                                      kubectl create -f /deploy.yml
                                      podname=$(kubectl get pods -o=jsonpath='{.items[0].metadata.name}')
                                      sleep 60
                                      kubectl cp /code/*.php $podname:/var/www/html/
                                      kubectl expose deploy deploy-task3 --port=80 --type=NodePort
                                      fi
				   ''')
     }
}


job('job3'){
                    triggers {
                                  upstream('job2', 'SUCCESS')
                            }
                     steps{
                                shell(''' port=$(kubectl get -o json service | jq .items[0].spec.ports[].nodePort)

export status=$(curl -s -i -w "%{http_code}" -o /dev/null 192.168.99.101:$port)
if [ $status==200 ]
then
exit 0
else 
sudo python3.6 /mail.py
exit 1
fi''')
    }
}

job('job4'){
                    scm{
                              github('adyraj/phpweb', 'master')
                       }
                    
                    steps{
                               shell('''cp -rvf * /myweb/
podname=$(kubectl get pods -o=jsonpath='{.items[0].metadata.name}')
kubectl cp /code/* $podname:/var/www/html/
)
    }
}

buildPipelineView('project-tsk6') {
    filterBuildQueue()
    filterExecutors()
    title('Project tsk6 CI Pipeline')
    displayedBuilds(3)
    selectedJob('job_1')
    alwaysAllowManualTrigger()
    showPipelineParameters()
    refreshFrequency(10)
}