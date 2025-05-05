// statemachine.js

const STATES = {
    DEFAULT: 'default',
    UNAUTHENTICATED: 'unauthenticated',
    AUTHENTICATED: 'authenticated'
  };
  
  const EVENTS = {
    SELECT_USER:    'selectUser',
    SELECT_ADMIN:   'selectAdmin',
    SIGNUP:         'signup',
    LOGIN:          'login',
    GET_MY_INFO:    'getMyInfo',
    UPDATE_MY_INFO: 'updateMyInfo',
    CHANGE_PW:      'changePW',
    WITHDRAW:       'withdraw',
    LOGOUT:         'logout'
  };
  
  const TRANSITIONS = {
    [STATES.DEFAULT]: {
      [EVENTS.SELECT_USER]:  STATES.UNAUTHENTICATED,
      [EVENTS.SELECT_ADMIN]: STATES.UNAUTHENTICATED
    },
    [STATES.UNAUTHENTICATED]: {
      [EVENTS.SIGNUP]: STATES.UNAUTHENTICATED,
      [EVENTS.LOGIN]:  STATES.AUTHENTICATED
    },
    [STATES.AUTHENTICATED]: {
      [EVENTS.GET_MY_INFO]:    STATES.AUTHENTICATED,
      [EVENTS.UPDATE_MY_INFO]: STATES.AUTHENTICATED,
      [EVENTS.CHANGE_PW]:      STATES.AUTHENTICATED,
      [EVENTS.WITHDRAW]:       STATES.UNAUTHENTICATED,
      [EVENTS.LOGOUT]:         STATES.DEFAULT        // 변경: 로그아웃 → default
    }
  };
  
  module.exports = {
    STATES,
    EVENTS,
    TRANSITIONS
  };
  