package com.diplome.game_recommendation.helpers.configuration;
// package com.diplome.game_recommendation.core.configuration;

// import org.quartz.*;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import com.diplome.game_recommendation.services.RecommendationJob;

// @Configuration
// public class QuartzConfig {

//     @Bean
//     public JobDetail rawgJob(){

//         return JobBuilder.newJob(RawgUpdateJob.class)
//                 .storeDurably()
//                 .build();
//     }

//     @Bean
//     public Trigger rawgTrigger(){
//         return TriggerBuilder.newTrigger()
//                 .forJob(rawgJob())
//                 .withSchedule(
//                         CronScheduleBuilder
//                                 .cronSchedule("0 0 4 * * ?")
//                 )
//                 .build();
//     }
//     @Bean
//     public JobDetail recommendationJob(){
//         return JobBuilder.newJob(RecommendationJob.class)
//                 .withIdentity("recommendationJob")
//                 .storeDurably()
//                 .build();
//     }

//     @Bean
//     public Trigger recommendationTrigger(){
//         return TriggerBuilder.newTrigger()
//                 .forJob(recommendationJob())
//                 .withIdentity("recommendationTrigger")
//                 .withSchedule(
//                         CronScheduleBuilder.cronSchedule("0 30 4 * * ?")
//                 )
//                 .build();
//     }
// }