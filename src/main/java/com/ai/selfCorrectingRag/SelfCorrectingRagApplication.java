package com.ai.selfCorrectingRag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SelfCorrectingRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(SelfCorrectingRagApplication.class, args);
	}
 /// for the first time to run chromadb after pulling the image use :docker run -d -p 8000:8000 --name chromadb chromadb/chroma
    // else just run docker start chromadb
}
