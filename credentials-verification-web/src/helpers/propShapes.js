import { string, shape, number, oneOfType, func, object } from 'prop-types';

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
  websiteLink: string,
  description: string,
  lastUpdate: number
};

export const connectionShape = {
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
