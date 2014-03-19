package krill;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

public class TestClass {
	
	@Test
	public void sortTest() {
		
		System.out.println("running test!");
		List <Integer> integers = new ArrayList <Integer>();
		integers.add(5);
		integers.add(2);
		integers.add(1);
		integers.add(20);

		Collections.sort(integers, new Comparator<Integer>(){

			@Override
			public int compare(Integer o1, Integer o2) {
				// TODO Auto-generated method stub
				return o1-o2;
			}
			
		});
		System.out.println(integers);
		assertEquals(1, 1);
		
	}

}
