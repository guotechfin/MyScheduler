package org.lu.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchedulerController {

	@Resource(name = "schedulerFactory")
	private SchedulerFactoryBean factory;

	@RequestMapping("/jobs")
	public List<Map<String, String>> jobs() throws SchedulerException,
			ParseException {
		Scheduler scheduler = factory.getObject();

		List<Map<String, String>> list = new ArrayList<>();
		for (String groupName : scheduler.getJobGroupNames()) {
			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
					.jobGroupEquals(groupName))) {
				String jobName = jobKey.getName();
				String jobGroup = jobKey.getGroup();

				@SuppressWarnings("unchecked")
				List<Trigger> triggers = (List<Trigger>) scheduler
						.getTriggersOfJob(jobKey);
				Date nextFireTime = triggers.get(0).getNextFireTime();

				Map<String, String> map = new HashMap<>();
				map.put("jobName", jobName);
				map.put("groupName", jobGroup);
				map.put("nextFireTime", nextFireTime.toString());
				list.add(map);
			}
		}
		return list;
	}

	@RequestMapping(value = "/removeJob/{jobName}")
	public String removeTask(@PathVariable String jobName)
			throws SchedulerException {
		Scheduler scheduler = factory.getObject();
		JobKey jobKey = new JobKey(jobName, "DEFAULT");
		scheduler.deleteJob(jobKey);
		return "task has been removed";
	}

}
