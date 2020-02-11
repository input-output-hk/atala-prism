import { string, shape, number, oneOf, oneOfType, func, object, bool } from 'prop-types';
import __ from 'lodash';
import { CONNECTION_STATUSES } from './constants';

export const connectionStatusesShape = __.values(CONNECTION_STATUSES);
export const connectionStatusesKeysShape = __.keys(CONNECTION_STATUSES);

export const subjectShape = {
  avatar: string,
  name: string,
  identityNumber: number,
  admissionDate: number,
  email: string,
  status: oneOf(connectionStatusesShape),
  id: string
};

export const groupShape = {
  icon: string,
  courseName: string,
  courseId: string,
  certificate: shape({
    certificateName: string,
    certificateId: string
  }),
  credential: shape({
    credentialName: string,
    credentialId: string
  }),
  lastUpdate: number
};

export const credentialSummaryShape = {
  id: string,
  user: shape({
    icon: string,
    name: string
  }),
  date: number,
  totalCredentials: number
};

export const paymentShape = {
  id: string,
  // Not returning from the backend
  user: shape({
    avatar: string,
    name: string
  }),
  date: number,
  amount: number,
  // Not returning from the backend
  currency: string,
  status: string,
  failureReason: string
};

export const refShape = oneOfType([
  func,
  shape({
    current: object
  })
]);

export const dateObjectShape = {
  day: number,
  month: number,
  year: number
};

export const studentShape = {
  admissiondate: dateObjectShape,
  connectionid: string,
  connectionstatus: oneOf(connectionStatusesShape),
  connectiontoken: string,
  fullname: string,
  id: string,
  issuerid: string,
  universityassignedid: string
};

export const infiniteTableProps = {
  loading: bool.isRequired,
  getMoreData: func.isRequired,
  hasMore: bool.isRequired
};

export const credentialShape = {
  icon: string,
  name: string,
  identityNumber: number,
  admissionDate: number,
  email: string,
  status: oneOf(['PENDING_CONNECTION', 'CONNECTED']),
  id: string
};
