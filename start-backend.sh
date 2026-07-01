nohup java -Xmx256m -jar jira-auth-service/target/jira-auth-service-1.0.0.jar > logs/auth-service.log 2>&1 &
echo  > platform-runtime/auth-service.pid
sleep 2

nohup java -Xmx256m -jar jira-user-service/target/jira-user-service-1.0.0.jar > logs/user-service.log 2>&1 &
echo  > platform-runtime/user-service.pid
sleep 2

nohup java -Xmx256m -jar jira-project-service/target/jira-project-service-1.0.0.jar > logs/project-service.log 2>&1 &
echo  > platform-runtime/project-service.pid
sleep 2

nohup java -Xmx256m -jar jira-issue-service/target/jira-issue-service-1.0.0.jar > logs/issue-service.log 2>&1 &
echo  > platform-runtime/issue-service.pid
sleep 2

nohup java -Xmx256m -jar jira-workflow-service/target/jira-workflow-service-1.0.0.jar > logs/workflow-service.log 2>&1 &
echo  > platform-runtime/workflow-service.pid
sleep 2

nohup java -Xmx256m -jar jira-comment-service/target/jira-comment-service-1.0.0.jar > logs/comment-service.log 2>&1 &
echo  > platform-runtime/comment-service.pid
sleep 2

nohup java -Xmx256m -jar jira-notification-service/target/jira-notification-service-1.0.0.jar > logs/notification-service.log 2>&1 &
echo  > platform-runtime/notification-service.pid
sleep 2

nohup java -Xmx256m -jar jira-search-service/target/jira-search-service-1.0.0.jar > logs/search-service.log 2>&1 &
echo  > platform-runtime/search-service.pid
sleep 2

nohup java -Xmx256m -jar jira-audit-service/target/jira-audit-service-1.0.0.jar > logs/audit-service.log 2>&1 &
echo  > platform-runtime/audit-service.pid
sleep 2

nohup java -Xmx256m -jar jira-attachment-service/target/jira-attachment-service-1.0.0.jar > logs/attachment-service.log 2>&1 &
echo  > platform-runtime/attachment-service.pid
sleep 2

nohup java -Xmx256m -jar jira-sprint-service/target/jira-sprint-service-1.0.0.jar > logs/sprint-service.log 2>&1 &
echo  > platform-runtime/sprint-service.pid
sleep 2

nohup java -Xmx256m -jar jira-plan-service/target/jira-plan-service-1.0.0.jar > logs/plan-service.log 2>&1 &
echo  > platform-runtime/plan-service.pid
sleep 2

nohup java -Xmx256m -jar jira-admin-service/target/jira-admin-service-1.0.0.jar > logs/admin-service.log 2>&1 &
echo  > platform-runtime/admin-service.pid
sleep 2

nohup java -Xmx256m -jar jira-migration-service/target/jira-migration-service-1.0.0.jar > logs/migration-service.log 2>&1 &
echo  > platform-runtime/migration-service.pid
sleep 2

