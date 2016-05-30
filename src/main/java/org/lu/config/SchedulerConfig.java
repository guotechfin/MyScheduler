package org.lu.config;

import org.lu.job.SampleJob;
import org.lu.util.AutowiringSpringBeanJobFactory;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;

import java.io.IOException;
import java.text.ParseException;
import java.util.Properties;

@Configuration
@ConditionalOnProperty(name = "quartz.enabled")
public class SchedulerConfig {

	@Bean
	public JobFactory jobFactory(ApplicationContext applicationContext) {
		AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		return jobFactory;
	}

	@Bean(name = "schedulerFactory")
	public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource,
			JobFactory jobFactory) throws IOException {
		SchedulerFactoryBean factory = new SchedulerFactoryBean();
		// this allows to update triggers in DB when updating settings in config
		// file:
		factory.setOverwriteExistingJobs(true);
		factory.setDataSource(dataSource);
		factory.setJobFactory(jobFactory);

		factory.setQuartzProperties(quartzProperties());
		return factory;
	}

	@Bean
	public Properties quartzProperties() throws IOException {
		PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
		propertiesFactoryBean.setLocation(new ClassPathResource(
				"/quartz.properties"));
		propertiesFactoryBean.afterPropertiesSet();
		return propertiesFactoryBean.getObject();
	}

	@Bean(name = "job")
	public JobDetail createJob() {
		JobDetailFactoryBean job = new JobDetailFactoryBean();

		job.setName("ScanJob");
		job.setJobClass(SampleJob.class);
		job.afterPropertiesSet();
		job.setDurability(true);
		JobDetail jobDetail = job.getObject();

		return jobDetail;
	}

	@Bean(name = "trigger")
	public CronTrigger createTrigger(@Qualifier("job") JobDetail job)
			throws ParseException {
		CustomCronTriggerFactoryBean trigger = new CustomCronTriggerFactoryBean();
		trigger.setJobDetail(job);
		trigger.setCronExpression("0/5 * * * * ?");
		trigger.setName("trigger1");
		trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
		trigger.afterPropertiesSet();
		CronTrigger tt = trigger.getObject();
		return tt;
	}

	@Bean
	public Scheduler startJob(
			@Qualifier("schedulerFactory") SchedulerFactoryBean factory,
			@Qualifier("job") JobDetail job,
			@Qualifier("trigger") CronTrigger trigger)
			throws SchedulerException, ParseException {
		Scheduler scheduler = factory.getObject();
		if (!checkJobExist(scheduler, job.getKey().getName())) {
			scheduler.scheduleJob(job, trigger);
			scheduler.start();
		}
		return scheduler;
	}

	private boolean checkJobExist(Scheduler scheduler, String jobName) {
		boolean exist = false;
		try {
			for (String groupName : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
						.jobGroupEquals(groupName))) {
					String jname = jobKey.getName();
					if (jname.equalsIgnoreCase(jobName)) {
						exist = true;
						return exist;
					}
				}
			}
		} catch (SchedulerException exp) {
			exp.printStackTrace();
		}

		return exist;
	}

	private static class CustomCronTriggerFactoryBean extends
			CronTriggerFactoryBean {
		@Override
		public void afterPropertiesSet() throws ParseException {
			super.afterPropertiesSet();
			// Remove the JobDetail element
			getJobDataMap().remove("jobDetail");
		}
	}
}
