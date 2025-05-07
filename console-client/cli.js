// console-client/cli.js

const axios = require('axios');
const readline = require('readline');
const { STATES, EVENTS, TRANSITIONS } = require('./stateMachine');

// 401 인터셉터를 위한 인스턴스 생성
const client = axios.create();

// 401 인터셉터 등록 : 401 응답 시 doRefresh() 호출 후 재시도
client.interceptors.response.use(
    response => {
        console.log(`✅ [RESPONSE] ${response.status} ${response.config.url}`); // 정상 응답도 찍기
        return response;
    },
    async error => {
      const { config, response } = error;
      console.log(`❌ [RESPONSE] ${response?.status} ${config.url}`);      // 에러 응답 찍기
      if (response?.status === 401 && !config._retry) {
        config._retry = true;
        await doRefresh();

        // 새로 발급받은 accessToken 헤더에 설정
        config.headers['Authorization'] = `Bearer ${tokens.accessToken}`;
        return client(config);
      }
      return Promise.reject(error);
    }
  );

const API = {
  signup:       'http://localhost:8080/api/user/signup',
  login:        'http://localhost:8080/api/auth/login',
  getMyInfo:    'http://localhost:8080/api/user/me',
  updateMyInfo: 'http://localhost:8080/api/user/me',
  changePW:     'http://localhost:8080/api/user/password',
  logout:       'http://localhost:8080/api/auth/logout',
  withdraw:     'http://localhost:8080/api/user/withdraw',
  refresh:      'http://localhost:8080/api/auth/refresh',
  matching:     'http://localhost:8080/api/matching/wait'
};

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
const question = q => new Promise(res => rl.question(q, res));

let state, actor, tokens;

async function run() {
  console.log('=== 콘솔 클라이언트 시작 ===');

  // 외부 루프: 세션이 끝나면 actor 선택 단계로 돌아감
  while (true) {
    // 초기화
    state = STATES.DEFAULT;
    actor = null;
    tokens = { accessToken: null, refreshToken: null };

    // 1) actor 선택
    while (state === STATES.DEFAULT) {
      console.log('\n== 역할 선택 ==');
      console.log('1) user');
      console.log('2) admin');
      const choice = (await question('> ')).trim();
      let ev = null;
      if (choice === '1') ev = EVENTS.SELECT_USER;
      if (choice === '2') ev = EVENTS.SELECT_ADMIN;
      if (ev && TRANSITIONS[state][ev]) {
        actor = ev === EVENTS.SELECT_USER ? 'user' : 'admin';
        state = TRANSITIONS[state][ev];
        console.log(`선택: ${actor} → 상태: ${state}`);
      } else {
        console.log('1 또는 2 중에서 선택하세요.');
      }
    }

    // 2) 세션 내부 루프
    let endSession = false;
    while (!endSession) {
      console.log(`\n[${state.toUpperCase()}] 가능한 액션:`);
      let menu = [];
      if (state === STATES.UNAUTHENTICATED) {
        menu = [
          { num: '1', label: 'signup',     event: EVENTS.SIGNUP },
          { num: '2', label: 'login',      event: EVENTS.LOGIN  }
        ];
      } else if (state === STATES.AUTHENTICATED) {
        menu = [
          { num: '1', label: 'getMyInfo',    event: EVENTS.GET_MY_INFO    },
          { num: '2', label: 'updateMyInfo', event: EVENTS.UPDATE_MY_INFO },
          { num: '3', label: 'changePW',     event: EVENTS.CHANGE_PW       },
          { num: '4', label: 'logout',       event: EVENTS.LOGOUT         },
          { num: '5', label: 'withdraw',     event: EVENTS.WITHDRAW       },
          { num: '6', label: 'matching',     event: EVENTS.MATCHING       }
        ];
      }
      menu.forEach(item => console.log(`${item.num}) ${item.label}`));
      const choice = (await question('> ')).trim();
      const selected = menu.find(item => item.num === choice);
      if (!selected) {
        console.log('올바른 번호를 입력하세요.');
        continue;
      }
      const action = selected.event;

      try {
        // 액션 수행
        switch(action) {
          case EVENTS.SIGNUP:         await doSignup();       break;
          case EVENTS.LOGIN:          await doLogin();        break;
          case EVENTS.GET_MY_INFO:    await doGetMyInfo();    break;
          case EVENTS.UPDATE_MY_INFO: await doUpdateMyInfo(); break;
          case EVENTS.CHANGE_PW:      await doChangePW();     break;
          case EVENTS.LOGOUT:         await doLogout();       break;
          case EVENTS.WITHDRAW:       await doWithdraw();     break;
          case EVENTS.MATCHING:       await doMatching();     break;
        }

        // 상태 전이
        const prevState = state;
        state = TRANSITIONS[state][action];
        console.log(`→ ${selected.label} → 상태: ${state}`);

        // 로그아웃 또는 탈퇴 시 세션 종료
        if (action === EVENTS.LOGOUT || action === EVENTS.WITHDRAW) {
          console.log(`\n${selected.label === EVENTS.LOGOUT ? '로그아웃' : '회원탈퇴'} 완료. 처음으로 돌아갑니다.`);
          endSession = true;
        }
      } catch(err) {
        console.error('에러:', err.response?.data || err.message);
        // 토큰 만료 등으로 401 발생하면 비인증 상태로 돌리고 세션 유지
        if (err.response?.status === 401 && state === STATES.AUTHENTICATED) {
          console.log('액세스 토큰 만료 → 다시 로그인하세요.');
          state = STATES.UNAUTHENTICATED;
        }
      }
    }

    // 세션이 끝나고 나서 다시 actor 선택 단계로 돌아감
  }
}

// --- helper functions (doSignup, doLogin, ...) 그대로 유지 ---

/**
 * 회원가입: 사용자 정보를 입력받아
 * POST /api/user/signup 호출 후 완료 메시지를 출력합니다.
 */
async function doSignup() {
    const username = await question('Username: ');
    const password = await question('Password: ');
    const name     = await question('Name: ');
    const phone    = await question('Phone: ');
    const email    = await question('Email: ');
    const role     = await question('Role (e.g. ROLE_USER): ');
    
    await axios.post(
      API.signup,
      { username, password, name, phone, email, role }
    );
    
    console.log('회원가입 완료');
}

/**
 * 로그인: username/password를 입력받아
 * POST /api/auth/login 호출 후
 * accessToken, refreshToken을 저장합니다.
 */
async function doLogin() {
    const username = await question('Username: ');
    const password = await question('Password: ');
    const res = await axios.post(API.login, { 
      username, 
      password 
    });
    tokens.accessToken  = res.data.accessToken;
    tokens.refreshToken = res.data.refreshToken;
    console.log('로그인 성공');
}

/**
 * 로그아웃: 저장된 refreshToken을 헤더에 담아
 * POST /api/auth/logout 호출 후
 * 로컬에 저장된 토큰을 모두 삭제합니다.
 */
async function doLogout() {
    await axios.post(
      API.logout,
      null,
      { headers: { Authorization: `Bearer ${tokens.refreshToken}` } }
    );
    tokens.accessToken = null;
    tokens.refreshToken = null;
    console.log('로그아웃 완료');
}

/**
 * 회원 탈퇴: 저장된 accessToken을 헤더에 담아
 * DELETE /api/user/withdraw 호출 후
 * 로컬에 저장된 토큰을 모두 삭제합니다.
 */
async function doWithdraw() {
    //await doRefresh();
    await client.delete(
      API.withdraw,
      { headers: { Authorization: `Bearer ${tokens.accessToken}` } }
    );
    tokens.accessToken = null;
    tokens.refreshToken = null;
    console.log('회원탈퇴 완료');
}

async function doGetMyInfo() {
    //await doRefresh();
    const res = await client.get(API.getMyInfo, {
      headers: { Authorization: `Bearer ${tokens.accessToken}` }
    });
    console.log('내 정보:', res.data);
}

async function doUpdateMyInfo() {
    //await doRefresh();
    const name  = await question('New Name: ');
    const phone = await question('New Phone: ');
    const email = await question('New Email: ');
    const res = await client.put(
      API.updateMyInfo,    
      { name, phone, email },
      { headers: { Authorization: `Bearer ${tokens.accessToken}` } }
    );
    console.log('수정된 정보:', res.data);
}

async function doChangePW() {
  //await doRefresh();
  const oldPassword = await question('Old Password: ');
  const newPassword = await question('New Password: ');
  await client.put(API.changePW,
    { oldPassword, newPassword },
    { headers: { Authorization: `Bearer ${tokens.accessToken}` } }
  );
  console.log('비밀번호 변경 완료');
}

async function doRefresh() {
    console.log("RFToken DEBUGGING!!");
    console.log(tokens.refreshToken);
    if (!tokens.refreshToken) {
      throw new Error('로그인 후에만 사용할 수 있습니다.');
    }
    const res = await axios.post(
      API.refresh, null,
      { headers: { Authorization: `Bearer ${tokens.refreshToken}` } }
    );
    tokens.accessToken = res.data.accessToken;
    console.log('⟳ 액세스 토큰 재발급 완료');
}

async function doMatching() {
  const count = await question("희망 매칭 인원 수(2~4): ");
  const res = await client.post(
    API.matching, 
    { count : parseInt(count) },
    { headers: { Authorization: `Bearer ${tokens.accessToken}` } }
  );

  const data = res.data;
  if (data.status === 'matched') {
      console.log(`✅ 매칭 완료! 채팅방 ID: ${data.roomId}`);
  } else {
      console.log(`⏳ 매칭 대기 중... 다른 사용자가 접속하면 자동으로 방이 만들어집니다.`);
  }
}


run();

