package com.hpcloud.mon.infrastructure.persistence;

import static org.testng.Assert.assertEquals;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Resources;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.domain.model.Alarm;
import com.hpcloud.mon.domain.model.SubAlarm;
import com.hpcloud.mon.domain.service.AlarmDAO;

/**
 * @author Jonathan Halterman
 */
@Test
public class AlarmDAOImplTest {
  private DBI db;
  private Handle handle;
  private AlarmDAO dao;

  @BeforeClass
  protected void setupClass() throws Exception {
    db = new DBI("jdbc:h2:mem:test;MODE=MySQL");
    handle = db.open();
    handle.execute(Resources.toString(getClass().getResource("alarm.sql"), Charset.defaultCharset()));
    dao = new AlarmDAOImpl(db);
  }

  @AfterClass
  protected void afterClass() {
    handle.close();
  }

  @BeforeMethod
  protected void beforeMethod() {
    handle.execute("truncate table alarm");
    handle.execute("truncate table sub_alarm");
    handle.execute("truncate table sub_alarm_dimension");
    handle.execute("truncate table alarm_action");

    handle.execute("insert into alarm (id, tenant_id, name, expression, state, created_at, updated_at) "
        + "values ('123', 'bob', '90% CPU', 'avg(hpcs.compute{disk=vda, instance_id=123, metric_name=cpu}) > 10', 'UNDETERMINED', NOW(), NOW())");
    handle.execute("insert into sub_alarm (id, alarm_id, function, namespace, operator, threshold, period, periods, created_at, updated_at) "
        + "values ('111', '123', 'AVG', 'hpcs.compute', 'GT', 10, 60, 1, NOW(), NOW())");
    handle.execute("insert into sub_alarm_dimension values ('111', 'instance_id', '123')");
    handle.execute("insert into sub_alarm_dimension values ('111', 'disk', 'vda')");
    handle.execute("insert into sub_alarm_dimension values ('111', 'metric_name', 'cpu')");
    handle.execute("insert into alarm_action values ('123', '29387234')");
    handle.execute("insert into alarm_action values ('123', '77778687')");
  }

  public void shouldFindById() {
    String expr = "avg(hpcs.compute{disk=vda, instance_id=123, metric_name=cpu}) > 10";
    Alarm expected = new Alarm("123", "bob", "90% CPU", AlarmExpression.of(expr),
        Arrays.asList(new SubAlarm("111", "123", AlarmSubExpression.of(expr))),
        AlarmState.UNDETERMINED);

    Alarm alarm = dao.findById("123");

    // Identity equality
    assertEquals(alarm, expected);
    assertEquals(alarm.getSubAlarms(), expected.getSubAlarms());
  }

  public void shouldUpdateState() {
    dao.updateState("123", AlarmState.ALARM);
    assertEquals(dao.findById("123").getState(), AlarmState.ALARM);
  }
}