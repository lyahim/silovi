# SiLoVi bridge

Bridge runs over Spring boot on Netty for fast serving.
Netty is an asynchronous event-driven network application framework for rapid development of maintainable high performance protocol servers & clients.
More about Netty on [Netty home](https://netty.io/)

Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications that you can "just run".
More about Spring boot on [Spring boot home](https://spring.io/projects/spring-boot)

## Configuration
Main configuration is available on root folder in config.properties file. More over own application properties available all Spring common properties what described [here](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html)

### Own properties
| property | description | required |default |
| --- | --- | --- | ---|
| root_log_folder | path for access log files. All files checked with 'file_name_regex_pattern' recursively under this folder. The search mechanism follows links too. **This config cannot be null or empty!** | true | - |
| file_name_regex_pattern | All selected file names going to pass against to this regex pattern. This is a simple regex pattern. By default select all files with log suffix. | true | .*.log |
| tail_lines_count | This value controls how many lines is loaded when file is selected. | true | 100 |
| more_lines_count | Where it is possible to load more lines this value controls the count of lines. Load more lines possible in search mode where there are gaps among founded lines or just opened file where it is possible to scroll back the lines. | true | 20 |
| file_check_interval | During 'live polling' mode this value controls the check interval to file changes. This value set in milliseconds. | true | 500 |
| auth_username | The system enables to protect bridge access with basic authentication over Spring authentication mechanism. This property set username. | false | - |
| auth_password | The system enables to protect bridge access with basic authentication over Spring authentication mechanism. This property set password. | false | - |