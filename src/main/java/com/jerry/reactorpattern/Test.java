package com.jerry.reactorpattern;

import java.io.UnsupportedEncodingException;

public class Test {
	public static void main(String[ ] args) {
		String s = "史"; 
		try {
			byte[] bytes = s.getBytes("utf-8");
			
			for (int i = 0; i<bytes.length;  ++i){
				System.out.println(""+bytes[i]);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
