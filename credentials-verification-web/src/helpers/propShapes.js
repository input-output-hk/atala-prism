import { string, shape, number, oneOfType, func, object } from 'prop-types';
import __ from 'lodash';
import { CONNECTION_STATUSES } from './constants';

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
  user: shape({
    avatar: string,
    name: string
  }),
  date: number,
  amount: number,
  currency: string
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

export const connectionStatusesShape = __.values(CONNECTION_STATUSES);

export const studentShape = {
  admissiondate: dateObjectShape,
  connectionid: string,
  connectionstatus: oneOfType(connectionStatusesShape),
  connectiontoken: string,
  fullname: string,
  id: string,
  issuerid: string,
  universityassignedid: string
};
