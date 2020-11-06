package com.chr.fservice.service.impl;

import com.chr.fservice.entity.Task;
import com.chr.fservice.mapper.TaskMapper;
import com.chr.fservice.service.ITaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author RAY
 * @since 2020-10-27
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {

}
