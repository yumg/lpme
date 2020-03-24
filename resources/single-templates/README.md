使用了IPLoM的单行模式分析结果


人工调整了openstack的单行分析结果：

* 类似 `Creating event network-vif-plugged abce-ace--fe-ffb for instance ef-e-cc--bff`的模式，被归纳为`Creating event network-vif-plugged <*> for instance <*>`
* `multiline.parser.Cleaner`对openstack的结果进行了清洗，处理了：
** `[instance <*> Total <*> <*> used . <*>`
** `[instance <*> <*> limit not specified defaulting to unlimited`
** `[instance <*> Instance <*> successfully.`
** `[instance <*> Took . seconds to <*> the instance on the hypervisor.`
