package com.replication.demo;

import com.replication.demo.entity.Computer;
import com.replication.demo.entity.ComputerType;
import com.replication.demo.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoApplicationTests {

	@PersistenceContext
	EntityManager em;

	@Test
	@Order(1)
	void contextLoads() {
	}

	@Test
	@Order(2)
	@DisplayName("최초 데이터 입력")
	@Transactional(readOnly = false)
	@Rollback(value = false)
	void init() {
		// 사용자 생성
		User user = new User();
		user.setAge(28);
		user.setName("leaf");

		// 컴퓨터 1 생성 및 저장
		List<Computer> computers = new ArrayList<>();
		Computer macCom = new Computer();
		macCom.setOS("Ventura");
		macCom.setType(ComputerType.MAC);
		macCom.setOwner(user);
		computers.add(macCom);
		em.persist(macCom);

		// 컴퓨터 2 생성 및 저장
		Computer windowCom = new Computer();
		windowCom.setOS("WINDOW 11");
		windowCom.setType(ComputerType.WINDOW);
		windowCom.setOwner(user);
		computers.add(windowCom);
		em.persist(windowCom);

		// 컴퓨터 사용자에 추가 후 사용자 저장
		user.setComputers(computers);
		em.persist(user);
		em.flush();
		em.clear();
	}

	@Test
	@Order(3)
	@DisplayName("사용자 조회 시 컴퓨터 잘 불러오는지 테스트")
	@Transactional(readOnly = true)
	void userQueryWithComputer() {
	    // given
		List<User> users = em.createQuery(
						"select u " +
								"from users as u " +
								"join u.computers as c", User.class)
				.getResultList();

	    // when
		User userInDB = users.get(0);

	    // then
		assertThat(userInDB.getName()).isEqualTo("leaf");
		assertThat(userInDB.getAge()).isEqualTo(28);
		assertThat(userInDB.getComputers()).hasSize(2);
		assertThat(userInDB.getComputers()).anyMatch(c -> c.getOS().equals("Ventura"));
		assertThat(userInDB.getComputers()).anyMatch(c -> c.getType().equals(ComputerType.MAC));
		assertThat(userInDB.getComputers()).anyMatch(c -> c.getOS().equals("WINDOW 11"));
		assertThat(userInDB.getComputers()).anyMatch(c -> c.getType().equals(ComputerType.WINDOW));
	}
}
