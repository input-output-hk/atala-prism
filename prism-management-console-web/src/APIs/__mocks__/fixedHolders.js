import { PENDING_CONNECTION, CONNECTED } from '../../helpers/constants';
import Logger from '../../helpers/Logger';

export const mockedHolders = [
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/travis_arnold/128.jpg',
    name: 'Kelli Brown',
    identityNumber: 63674,
    admissionDate: 1571206230,
    email: 'Randall.Wolff@yahoo.com',
    status: PENDING_CONNECTION,
    id: 'p',
    key: 'p'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/superoutman/128.jpg',
    name: 'Lea Collins',
    identityNumber: 96202,
    admissionDate: 1571180014,
    email: 'Autumn_Kunde@yahoo.com',
    status: PENDING_CONNECTION,
    id: 'i',
    key: 'i'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/lewisainslie/128.jpg',
    name: 'Alexa Johnson',
    identityNumber: 78828,
    admissionDate: 1571206287,
    email: 'Gail.Kovacek95@gmail.com',
    status: CONNECTED,
    id: 'j',
    key: 'j'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/al_li/128.jpg',
    name: 'Jarod Will',
    identityNumber: 21051,
    admissionDate: 1571226357,
    email: 'Nat.Koss@yahoo.com',
    status: CONNECTED,
    id: 'b',
    key: 'b'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/calebjoyce/128.jpg',
    name: 'Robert Zemlak',
    identityNumber: 81378,
    admissionDate: 1571179997,
    email: 'Emma19@yahoo.com',
    status: CONNECTED,
    id: 'q',
    key: 'q'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/sircalebgrove/128.jpg',
    name: 'Norma VonRueden',
    identityNumber: 48628,
    admissionDate: 1571200728,
    email: 'Greyson_Emard@hotmail.com',
    status: PENDING_CONNECTION,
    id: 'z',
    key: 'z'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/geshan/128.jpg',
    name: 'Otis Heaney',
    identityNumber: 47145,
    admissionDate: 1571232206,
    email: 'Chelsie.Gerhold89@yahoo.com',
    status: PENDING_CONNECTION,
    id: 'h',
    key: 'h'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/deeenright/128.jpg',
    name: 'Dariana Rodriguez',
    identityNumber: 8643,
    admissionDate: 1571186724,
    email: 'Camila_Maggio26@gmail.com',
    status: PENDING_CONNECTION,
    id: 'c',
    key: 'c'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/kudretkeskin/128.jpg',
    name: 'Tyrique Pfeffer',
    identityNumber: 24147,
    admissionDate: 1571195744,
    email: 'Rodrigo62@yahoo.com',
    status: PENDING_CONNECTION,
    id: '4',
    key: '4'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/codepoet_ru/128.jpg',
    name: 'Fermin Hauck',
    identityNumber: 20963,
    admissionDate: 1571217949,
    email: 'Jolie50@hotmail.com',
    status: PENDING_CONNECTION,
    id: 'w',
    key: 'w'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/danthms/128.jpg',
    name: 'Colby Hayes',
    identityNumber: 18656,
    admissionDate: 1571225532,
    email: 'Ernie_Romaguera@gmail.com',
    status: PENDING_CONNECTION,
    id: 'z1',
    key: 'z1'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/samgrover/128.jpg',
    name: 'Shyann Goldner',
    identityNumber: 69575,
    admissionDate: 1571190154,
    email: 'Clifford27@gmail.com',
    status: PENDING_CONNECTION,
    id: 'x',
    key: 'x'
  },
  {
    avatar: 'https://s3.amazonaws.com/uifaces/faces/twitter/myastro/128.jpg',
    name: 'Roxane Langosh',
    identityNumber: 73621,
    admissionDate: 1571173229,
    email: 'Mercedes.Cassin22@yahoo.com',
    status: PENDING_CONNECTION,
    id: 'c1',
    key: 'c1'
  }
];

export const getHolders = ({ identityNumber = 0, name = '', status = '', pageSize, offset }) =>
  new Promise(resolve => {
    const filteredHolders = mockedHolders.filter(
      ({ identityNumber: holderIdentityNumber, status: holderStatus, name: holderName }) =>
        (!status || status === holderStatus) &&
        holderName.toLowerCase().includes(name.toLowerCase()) &&
        holderIdentityNumber.toString().includes(identityNumber.toString())
    );

    const skip = offset * pageSize;
    resolve({
      holders: filteredHolders.slice(skip, skip + pageSize),
      holderCount: filteredHolders.length
    });
  });

export const inviteHolder = ({ id }) => {
  Logger.info('Invited the user with the id: ', id);
  return Promise.resolve(200);
};
