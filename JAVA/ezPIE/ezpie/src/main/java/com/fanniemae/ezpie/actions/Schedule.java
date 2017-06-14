/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 *
 */

package com.fanniemae.ezpie.actions;

import java.time.LocalDateTime;
import java.util.HashMap;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.CalculateSchedule;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-06-04
 *
 */

public class Schedule extends Action {

	public Schedule(SessionManager session, Element action) {
		super(session, action, true);
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		CalculateSchedule calc = new CalculateSchedule(_session,_action);
		LocalDateTime newSchedule = calc.nextScheduledRun();
		if (newSchedule != null) {
			// See if any schedule is already set
			String sSchedule = _session.getTokenValue("Schedule", _name);
			if (StringUtilities.isNotNullOrEmpty(sSchedule)) {
				LocalDateTime currentSchedule = StringUtilities.toDateTime(sSchedule, LocalDateTime.MIN);
				if ((currentSchedule != LocalDateTime.MIN) && newSchedule.isBefore(currentSchedule)) {
					_session.addToken("Schedule", _name, newSchedule.toString());
				} else {
					_session.addLogMessage("", "Result", String.format("Keeping earlier schedule (%s), this schedule returned %s.", currentSchedule.toString(), newSchedule.toString()));
				}
			} else {
				_session.addToken("Schedule", _name, newSchedule.toString());
						}

		} else {
			_session.addLogMessage("", "Result", calc.getReason());
		}
		return null;
	}

}
