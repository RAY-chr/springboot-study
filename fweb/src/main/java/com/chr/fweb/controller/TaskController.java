package com.chr.fweb.controller;


import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.chr.fservice.config.DataSourceConfig;
import com.chr.fservice.config.DataSourceContext;
import com.chr.fservice.config.OptionalDataSource;
import com.chr.fservice.service.IBookService;
import com.chr.fweb.config.IpLimit;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author RAY
 * @since 2020-10-27
 */
@RestController
public class TaskController {

    private static final String ORDER_KEY = "getOrder";

    @Autowired
    private DataSourceConfig.RouteDataSource routeDataSource;

    @Autowired
    private IBookService bookService;

    @PostConstruct
    public void initFlowQpsRule() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource(ORDER_KEY);
        // QPS控制在2以内
        rule.setCount(1);
        // QPS限流
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }


    @RequestMapping("/getOrder")
    public String getOrders() {
        Entry entry = null;
        try {
            entry = SphU.entry(ORDER_KEY);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "该服务接口已经达到上线!";
        } finally {
            // SphU.entry(xxx) 需要与 entry.exit() 成对出现,否则会导致调用链记录异常
            if (entry != null) {
                entry.exit();
            }
        }
    }

    @SentinelResource(value = ORDER_KEY, blockHandler = "getOrderQpsException")
    @RequestMapping("/getOrderAnnotation")
    public String getOrderAnnotation() {
        return "success";
    }

    @RequestMapping("/getOrderRedis")
    @IpLimit(times = 1, expire = 1)
    public String getOrderRedis() {
        return "success";
    }

    /**
     * 运行的时候添加数据源
     * @return
     * @throws Exception
     */
    @RequestMapping("/addDataSource")
    public String addDataSource() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDataSource(new OptionalDataSource());
        config.setMinimumIdle(1);
        HikariDataSource hikariDataSource = new HikariDataSource(config);
        routeDataSource.addDataSource("new", hikariDataSource);
        return "success";
    }

    @RequestMapping("/choseDataSource")
    public String choseDataSource() {
        DataSourceContext.setDataSource("new");
        bookService.list();
        DataSourceContext.clearCache();
        return "success";
    }

    /**
     * 被限流后返回的提示
     *
     * @param e
     * @return
     */
    public String getOrderQpsException(BlockException e) {
        e.printStackTrace();
        return "该接口已经被限流啦!";
    }


}
