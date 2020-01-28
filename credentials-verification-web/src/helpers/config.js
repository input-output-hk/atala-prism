import { USER_ROLE } from './constants';

export const config = {
  userRole: localStorage.getItem(USER_ROLE)
};
