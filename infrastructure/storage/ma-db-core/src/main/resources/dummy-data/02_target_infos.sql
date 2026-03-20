-- TargetInfo 150건 테스트 데이터
-- registerEmail은 Member에 존재하는 이메일 참조
-- name은 실제 Member에 있는 이름(매칭됨) + 없는 이름(매칭 안됨) 혼합
-- middleNumber, lastNumber, year, month, day, region은 일부 null 허용
INSERT INTO TARGET_INFOS (REGISTER_EMAIL, NAME, TARGET_GENDER, MIDDLE_NUMBER, LAST_NUMBER, YEAR, MONTH, DAY, REGION, CREATED_BY, LAST_MODIFIED_BY, DELETED) VALUES
-- 김민수가 찾는 사람들
('kim.minsu01@gmail.com', '이지연', 'FEMALE', '1234', '0002', 1999, 7, 22, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kim.minsu01@gmail.com', '최소연', 'FEMALE', NULL, NULL, 2000, 1, NULL, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kim.minsu01@gmail.com', '한민지', 'FEMALE', '1234', '0008', NULL, NULL, NULL, 'GWANGJU', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 이지연이 찾는 사람들
('lee.jiyeon02@gmail.com', '김민수', 'MALE', '1234', '0001', 1998, 3, 15, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lee.jiyeon02@gmail.com', '윤서준', 'MALE', NULL, NULL, 1997, 12, 25, NULL, 'MEET_AGAIN', 'MEET_AGAIN', false),
('lee.jiyeon02@gmail.com', '박성민', 'MALE', '5678', '9012', 1998, 5, 10, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 박지훈이 찾는 사람들
('park.jihoon03@naver.com', '강유나', 'FEMALE', '1234', '0006', 1999, 9, 3, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('park.jihoon03@naver.com', '서예린', 'FEMALE', NULL, NULL, NULL, NULL, NULL, NULL, 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 최소연이 찾는 사람들
('choi.soyeon04@naver.com', '박지훈', 'MALE', '1234', '0003', 1997, 11, 8, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('choi.soyeon04@naver.com', '정우진', 'MALE', NULL, '0005', 1998, NULL, NULL, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('choi.soyeon04@naver.com', '김현석', 'MALE', '3333', '4444', 1999, 2, 14, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 정우진이 찾는 사람들
('jung.woojin05@gmail.com', '오하영', 'FEMALE', '1234', '0010', 1999, 2, 14, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('jung.woojin05@gmail.com', '양다현', 'FEMALE', NULL, NULL, 2000, 3, 7, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 강유나가 찾는 사람들
('kang.yuna06@gmail.com', '박지훈', 'MALE', '1234', '0003', 1997, 11, 8, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kang.yuna06@gmail.com', '황태현', 'MALE', '1234', '0013', 1998, 1, 20, 'ULSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kang.yuna06@gmail.com', '이준호', 'MALE', '9999', '8888', 1997, 3, 5, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 윤서준이 찾는 사람들
('yoon.seojun07@naver.com', '이지연', 'FEMALE', '1234', '0002', 1999, 7, 22, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yoon.seojun07@naver.com', '송은지', 'FEMALE', NULL, NULL, NULL, NULL, NULL, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yoon.seojun07@naver.com', '전수빈', 'FEMALE', NULL, '0014', 1999, 11, 7, NULL, 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 한민지가 찾는 사람들
('han.minji08@gmail.com', '신동현', 'MALE', '1234', '0009', 1998, 8, 29, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('han.minji08@gmail.com', '임준혁', 'MALE', NULL, NULL, 1997, 6, 5, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 신동현이 찾는 사람들
('shin.donghyun09@naver.com', '한민지', 'FEMALE', '1234', '0008', 2000, 4, 17, 'GWANGJU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('shin.donghyun09@naver.com', '노지원', 'FEMALE', '1234', '0016', 2000, 8, 23, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('shin.donghyun09@naver.com', '김하늘', 'FEMALE', '1111', '2222', 1999, 4, 1, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 오하영이 찾는 사람들
('oh.hayoung10@gmail.com', '정우진', 'MALE', '1234', '0005', 1998, 5, 12, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('oh.hayoung10@gmail.com', '김민수', 'MALE', NULL, NULL, 1998, 3, NULL, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 임준혁이 찾는 사람들
('lim.junhyuk11@gmail.com', '송은지', 'FEMALE', '1234', '0012', 2000, 10, 11, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lim.junhyuk11@gmail.com', '안소희', 'FEMALE', NULL, NULL, NULL, NULL, NULL, 'JEOLLA_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lim.junhyuk11@gmail.com', '박서연', 'FEMALE', '7777', '6666', 1999, 8, 20, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 송은지가 찾는 사람들
('song.eunji12@naver.com', '유찬우', 'MALE', '1234', '0023', 1997, 2, 19, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('song.eunji12@naver.com', '백성훈', 'MALE', NULL, NULL, 1998, 12, 1, NULL, 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 황태현이 찾는 사람들
('hwang.taehyun13@gmail.com', '강유나', 'FEMALE', '1234', '0006', 1999, 9, 3, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('hwang.taehyun13@gmail.com', '서예린', 'FEMALE', '1234', '0018', 1999, 6, 18, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('hwang.taehyun13@gmail.com', '한정연', 'FEMALE', NULL, NULL, NULL, NULL, NULL, NULL, 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 전수빈이 찾는 사람들
('jeon.subin14@naver.com', '윤서준', 'MALE', '1234', '0007', 1997, 12, 25, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('jeon.subin14@naver.com', '조민서', 'MALE', NULL, '0021', 1998, 7, 11, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 권현우가 찾는 사람들
('kwon.hyunwoo15@gmail.com', '노지원', 'FEMALE', '1234', '0016', 2000, 8, 23, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kwon.hyunwoo15@gmail.com', '서미래', 'FEMALE', NULL, NULL, 2000, 5, 18, 'CHUNGCHEONG_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kwon.hyunwoo15@gmail.com', '정다은', 'FEMALE', '4321', '8765', 1998, 9, 9, 'GANGWON_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 노지원이 찾는 사람들
('noh.jiwon16@gmail.com', '신동현', 'MALE', '1234', '0009', 1998, 8, 29, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('noh.jiwon16@gmail.com', '권현우', 'MALE', '1234', '0015', 1997, 4, 2, 'GANGWON_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 백성훈이 찾는 사람들
('baek.sunghoon17@naver.com', '서예린', 'FEMALE', '1234', '0018', 1999, 6, 18, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('baek.sunghoon17@naver.com', '고서연', 'FEMALE', NULL, NULL, 2000, 6, 21, 'GWANGJU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('baek.sunghoon17@naver.com', '윤서현', 'FEMALE', '5555', '6666', 1998, 11, 30, 'CHUNGCHEONG_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 서예린이 찾는 사람들
('seo.yerin18@gmail.com', '황태현', 'MALE', '1234', '0013', 1998, 1, 20, 'ULSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('seo.yerin18@gmail.com', '문재현', 'MALE', NULL, NULL, 1997, 9, 14, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 문재현이 찾는 사람들
('moon.jaehyun19@naver.com', '양다현', 'FEMALE', '1234', '0020', 2000, 3, 7, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('moon.jaehyun19@naver.com', '배채영', 'FEMALE', NULL, '0026', 1999, 1, 9, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('moon.jaehyun19@naver.com', '차은우', 'FEMALE', '2222', '3333', 1999, 5, 5, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 양다현이 찾는 사람들
('yang.dahyun20@gmail.com', '조민서', 'MALE', '1234', '0021', 1998, 7, 11, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yang.dahyun20@gmail.com', '정우진', 'MALE', NULL, NULL, NULL, NULL, NULL, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 조민서가 찾는 사람들
('cho.minseo21@gmail.com', '양다현', 'FEMALE', '1234', '0020', 2000, 3, 7, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('cho.minseo21@gmail.com', '최유진', 'FEMALE', NULL, NULL, 1999, 4, 25, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('cho.minseo21@gmail.com', '강민지', 'FEMALE', '8888', '7777', 2000, 2, 14, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 안소희가 찾는 사람들
('ahn.sohee22@naver.com', '남지환', 'MALE', '1234', '0025', 1998, 10, 30, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('ahn.sohee22@naver.com', '류동진', 'MALE', NULL, NULL, 1997, 8, 16, 'CHUNGCHEONG_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 유찬우가 찾는 사람들
('yoo.chanwoo23@gmail.com', '홍나영', 'FEMALE', '1234', '0024', 2000, 12, 5, 'GYEONGSANG_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yoo.chanwoo23@gmail.com', '송은지', 'FEMALE', '1234', '0012', 2000, 10, 11, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yoo.chanwoo23@gmail.com', '박지현', 'FEMALE', '6543', '2109', 1998, 7, 7, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 홍나영이 찾는 사람들
('hong.nayoung24@naver.com', '유찬우', 'MALE', '1234', '0023', 1997, 2, 19, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('hong.nayoung24@naver.com', '하영민', 'MALE', NULL, NULL, 1998, 4, 8, 'JEJU_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 남지환이 찾는 사람들
('nam.jihwan25@gmail.com', '배채영', 'FEMALE', '1234', '0026', 1999, 1, 9, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('nam.jihwan25@gmail.com', '우지영', 'FEMALE', NULL, NULL, 1999, 12, 14, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('nam.jihwan25@gmail.com', '이수정', 'FEMALE', '1357', '2468', 1997, 3, 22, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 배채영이 찾는 사람들
('bae.chaeyoung26@gmail.com', '김승호', 'MALE', '1234', '0031', 1997, 3, 28, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('bae.chaeyoung26@gmail.com', '문재현', 'MALE', '1234', '0019', 1997, 9, 14, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 류동진이 찾는 사람들
('ryu.dongjin27@naver.com', '안소희', 'FEMALE', '1234', '0022', 1999, 5, 26, 'JEOLLA_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('ryu.dongjin27@naver.com', '고서연', 'FEMALE', '1234', '0028', 2000, 6, 21, 'GWANGJU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('ryu.dongjin27@naver.com', '김영주', 'FEMALE', '4444', '5555', 1998, 10, 10, 'CHUNGCHEONG_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 고서연이 찾는 사람들
('go.seoyeon28@gmail.com', '류동진', 'MALE', '1234', '0027', 1997, 8, 16, 'CHUNGCHEONG_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('go.seoyeon28@gmail.com', '백성훈', 'MALE', NULL, NULL, NULL, NULL, NULL, NULL, 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 하영민이 찾는 사람들
('ha.youngmin29@naver.com', '홍나영', 'FEMALE', '1234', '0024', 2000, 12, 5, 'GYEONGSANG_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('ha.youngmin29@naver.com', '노은비', 'FEMALE', NULL, '0046', 1999, 8, 27, 'GANGWON_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 우지영이 찾는 사람들
('woo.jiyoung30@gmail.com', '김도현', 'MALE', '1234', '0061', 1998, 5, 24, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('woo.jiyoung30@gmail.com', '임준혁', 'MALE', '1234', '0011', 1997, 6, 5, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('woo.jiyoung30@gmail.com', '송진우', 'MALE', '3210', '6789', 1999, 1, 1, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 김승호가 찾는 사람들
('kim.seungho31@naver.com', '이혜진', 'FEMALE', '1234', '0032', 2000, 9, 19, 'GYEONGSANG_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kim.seungho31@naver.com', '최유진', 'FEMALE', NULL, NULL, 1999, 4, 25, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 이혜진이 찾는 사람들
('lee.hyejin32@gmail.com', '김승호', 'MALE', '1234', '0031', 1997, 3, 28, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lee.hyejin32@gmail.com', '박용준', 'MALE', '1234', '0033', 1998, 11, 3, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lee.hyejin32@gmail.com', '정태영', 'MALE', '2468', '1357', 1997, 6, 15, 'GYEONGSANG_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 박용준이 찾는 사람들
('park.yongjun33@naver.com', '이혜진', 'FEMALE', '1234', '0032', 2000, 9, 19, 'GYEONGSANG_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('park.yongjun33@naver.com', '강수진', 'FEMALE', NULL, NULL, 2000, 2, 28, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 최유진이 찾는 사람들
('choi.yujin34@gmail.com', '조민서', 'MALE', '1234', '0021', 1998, 7, 11, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('choi.yujin34@gmail.com', '윤도윤', 'MALE', '1234', '0037', 1998, 6, 13, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('choi.yujin34@gmail.com', '한성준', 'MALE', '9876', '5432', 1999, 8, 20, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 정시우가 찾는 사람들
('jung.siwoo35@naver.com', '전연우', 'FEMALE', '1234', '0044', 2000, 11, 15, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('jung.siwoo35@naver.com', '송해인', 'FEMALE', NULL, NULL, 1999, 3, 6, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 강수진이 찾는 사람들
('kang.sujin36@gmail.com', '윤도윤', 'MALE', '1234', '0037', 1998, 6, 13, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kang.sujin36@gmail.com', '신현재', 'MALE', '1234', '0039', 1997, 1, 17, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kang.sujin36@gmail.com', '오민재', 'MALE', '7890', '1234', 1998, 4, 4, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 윤도윤이 찾는 사람들
('yoon.doyun37@naver.com', '강수진', 'FEMALE', '1234', '0036', 2000, 2, 28, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yoon.doyun37@naver.com', '최유진', 'FEMALE', '1234', '0034', 1999, 4, 25, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 한정연이 찾는 사람들
('han.jeongyeon38@gmail.com', '황태현', 'MALE', '1234', '0013', 1998, 1, 20, 'ULSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('han.jeongyeon38@gmail.com', '임재민', 'MALE', NULL, NULL, 1998, 9, 21, 'GWANGJU', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 신현재가 찾는 사람들
('shin.hyunjae39@naver.com', '오가영', 'FEMALE', '1234', '0040', 2000, 7, 9, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('shin.hyunjae39@naver.com', '송지윤', 'FEMALE', '1234', '0072', 2000, 3, 21, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('shin.hyunjae39@naver.com', '나은별', 'FEMALE', '1010', '2020', 1999, 7, 7, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 오가영이 찾는 사람들
('oh.gayoung40@gmail.com', '신현재', 'MALE', '1234', '0039', 1997, 1, 17, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('oh.gayoung40@gmail.com', '문상우', 'MALE', NULL, NULL, 1998, 12, 22, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 임재민이 찾는 사람들
('lim.jaemin41@naver.com', '한정연', 'FEMALE', '1234', '0038', 1999, 10, 2, 'JEOLLA_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lim.jaemin41@naver.com', '고나연', 'FEMALE', NULL, NULL, NULL, NULL, NULL, 'GWANGJU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lim.jaemin41@naver.com', '장서윤', 'FEMALE', '6060', '7070', 2000, 1, 15, 'GWANGJU', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 송해인이 찾는 사람들
('song.haein42@gmail.com', '정시우', 'MALE', '1234', '0035', 1997, 7, 7, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('song.haein42@gmail.com', '백준성', 'MALE', '1234', '0047', 1997, 10, 10, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 황건우가 찾는 사람들
('hwang.gunwoo43@naver.com', '한슬기', 'FEMALE', '1234', '0068', 2000, 12, 29, 'ULSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('hwang.gunwoo43@naver.com', '전혜원', 'FEMALE', NULL, NULL, 1999, 2, 7, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('hwang.gunwoo43@naver.com', '이예은', 'FEMALE', '3030', '4040', 1998, 6, 25, 'ULSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 전연우가 찾는 사람들
('jeon.yeonwoo44@gmail.com', '정시우', 'MALE', NULL, '0035', 1997, 7, NULL, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('jeon.yeonwoo44@gmail.com', '황건우', 'MALE', '1234', '0043', 1997, 5, 31, 'ULSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 권지혁이 찾는 사람들
('kwon.jihyuk45@naver.com', '노은비', 'FEMALE', '1234', '0046', 1999, 8, 27, 'GANGWON_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kwon.jihyuk45@naver.com', '서진솔', 'FEMALE', NULL, NULL, 1999, 12, 3, 'CHUNGCHEONG_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 노은비가 찾는 사람들
('noh.eunbi46@gmail.com', '권지혁', 'MALE', '1234', '0045', 1998, 2, 4, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('noh.eunbi46@gmail.com', '하영민', 'MALE', '1234', '0029', 1998, 4, 8, 'JEJU_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('noh.eunbi46@gmail.com', '정유빈', 'MALE', '5050', '6060', 1997, 11, 11, 'GANGWON_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 백준성이 찾는 사람들
('baek.junseong47@naver.com', '송해인', 'FEMALE', '1234', '0042', 1999, 3, 6, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('baek.junseong47@naver.com', '양예지', 'FEMALE', NULL, NULL, 1999, 7, 4, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 서미래가 찾는 사람들
('seo.mirae48@gmail.com', '백성훈', 'MALE', '1234', '0017', 1998, 12, 1, 'CHUNGCHEONG_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('seo.mirae48@gmail.com', '남경수', 'MALE', NULL, NULL, 1997, 6, 9, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('seo.mirae48@gmail.com', '최민규', 'MALE', '8080', '9090', 1999, 3, 3, 'CHUNGCHEONG_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 문상우가 찾는 사람들
('moon.sangwoo49@naver.com', '오가영', 'FEMALE', '1234', '0040', 2000, 7, 9, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('moon.sangwoo49@naver.com', '양하은', 'FEMALE', '1234', '0080', 2000, 6, 15, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 양예지가 찾는 사람들
('yang.yeji50@gmail.com', '백준성', 'MALE', '1234', '0047', 1997, 10, 10, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yang.yeji50@gmail.com', '조태양', 'MALE', NULL, NULL, 1997, 11, 28, 'JEOLLA_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yang.yeji50@gmail.com', '김세훈', 'MALE', '2020', '3030', 1998, 8, 8, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
-- 추가 데이터 (다양한 사용자)
('cho.taeyang51@naver.com', '안라은', 'FEMALE', '1234', '0052', 2000, 4, 3, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('ahn.raeun52@gmail.com', '조태양', 'MALE', '1234', '0051', 1997, 11, 28, 'JEOLLA_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yoo.inwoo53@naver.com', '홍시내', 'FEMALE', '1234', '0054', 1999, 1, 22, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('hong.sinae54@gmail.com', '유인우', 'MALE', '1234', '0053', 1998, 8, 15, 'GYEONGSANG_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('nam.kyungsoo55@naver.com', '배지은', 'FEMALE', '1234', '0056', 2000, 10, 27, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('bae.jieun56@gmail.com', '남경수', 'MALE', '1234', '0055', 1997, 6, 9, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('ryu.sunwoo57@naver.com', '고나연', 'FEMALE', '1234', '0058', 1999, 9, 30, 'GWANGJU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('go.nayeon58@gmail.com', '류선우', 'MALE', '1234', '0057', 1998, 3, 14, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('ha.seungmin59@naver.com', '우정희', 'FEMALE', NULL, NULL, 2000, 1, 13, 'SEJONG', 'MEET_AGAIN', 'MEET_AGAIN', false),
('woo.jeonghee60@gmail.com', '하승민', 'MALE', '1234', '0059', 1997, 12, 6, 'CHUNGCHEONG_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kim.dohyun61@naver.com', '이소영', 'FEMALE', '1234', '0062', 1999, 11, 16, 'GYEONGSANG_BUK_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lee.soyoung62@gmail.com', '김도현', 'MALE', '1234', '0061', 1998, 5, 24, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('park.hyunwoo63@naver.com', '최아름', 'FEMALE', NULL, NULL, 2000, 8, 1, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('choi.areum64@gmail.com', '박현우', 'MALE', '1234', '0063', 1997, 2, 8, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('jung.minho65@naver.com', '강보람', 'FEMALE', '1234', '0066', 1999, 4, 12, 'JEOLLA_NAM_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kang.boram66@gmail.com', '정민호', 'MALE', '1234', '0065', 1998, 10, 19, 'DAEGU', 'MEET_AGAIN', 'MEET_AGAIN', false),
('yoon.jungwoo67@naver.com', '한슬기', 'FEMALE', NULL, NULL, NULL, NULL, NULL, 'ULSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('han.seulgi68@gmail.com', '윤정우', 'MALE', '1234', '0067', 1997, 7, 23, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('shin.jaewon69@naver.com', '오선희', 'FEMALE', '1234', '0070', 1999, 6, 28, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false),
('oh.sunhee70@gmail.com', '신재원', 'MALE', '1234', '0069', 1998, 1, 5, 'GANGWON_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('lim.seonghwan71@naver.com', '송지윤', 'FEMALE', '1234', '0072', 2000, 3, 21, 'GYEONGGI_DO', 'MEET_AGAIN', 'MEET_AGAIN', false),
('song.jiyun72@gmail.com', '임성환', 'MALE', NULL, NULL, 1997, 9, 9, 'INCHEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('hwang.wooseok73@naver.com', '전혜원', 'FEMALE', '1234', '0074', 1999, 2, 7, 'BUSAN', 'MEET_AGAIN', 'MEET_AGAIN', false),
('jeon.hyewon74@gmail.com', '황우석', 'MALE', '1234', '0073', 1998, 11, 14, 'DAEJEON', 'MEET_AGAIN', 'MEET_AGAIN', false),
('kwon.youngho75@naver.com', '노윤아', 'FEMALE', '1234', '0076', 2000, 9, 6, 'SEOUL', 'MEET_AGAIN', 'MEET_AGAIN', false);
