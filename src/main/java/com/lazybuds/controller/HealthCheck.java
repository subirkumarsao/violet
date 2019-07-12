package com.lazybuds.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HealthCheck {

	@RequestMapping(value="/ping",method=RequestMethod.GET)
	public @ResponseBody String ping() {
		return "pong";
	}
}
