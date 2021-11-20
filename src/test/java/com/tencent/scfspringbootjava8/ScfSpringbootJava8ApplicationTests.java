package com.tencent.scfspringbootjava8;

import com.tencent.scfspringbootjava8.task.jd_fruit;
import com.tencent.scfspringbootjava8.task.jd_speed_redpocke;
import com.tencent.scfspringbootjava8.task.jd_speed_signfaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class ScfSpringbootJava8ApplicationTests {

	@Autowired
	jd_speed_redpocke jd_speed_redpocke;

	@Autowired
	jd_speed_signfaker jd_speed_signfaker;

	@Autowired
	jd_fruit jd_fruit;

	@Test
	void contextLoads() {
//		jd_speed_redpocke.invite("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_redpocke.sign("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_redpocke.reward_query("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_redpocke.redPacket("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_redpocke.getPacketList("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_redpocke.signPrizeDetailList("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.wheelsHome("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.invite("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.invite2("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.apTaskList("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.richManIndex("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.taskList("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.taskList("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.queryJoy("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_speed_signfaker.cash("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_fruit.initForFarm("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
//		jd_fruit.taskInitForFarm("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
		jd_fruit.jdFruit("pt_key=AAJhiS6TADD5CC8v8fWf63rYbm7ygR8BQT_jQen-j0uPDgvvGiDtvaedfbM_FUYBogoK1S1YLN0;pt_pin=jd_4918fa77c820d;");
	}

}
