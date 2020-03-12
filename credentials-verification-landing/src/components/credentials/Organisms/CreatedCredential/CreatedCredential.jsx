import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { useTranslation } from 'react-i18next';
import CredentialData from '../../../common/Atoms/CredentialData/CredentialData';
import { UserContext } from '../../../providers/userContext';
import CredentialIDTemplate from '../../Molecules/CredentialIdTemplate/CredentialIdTemplate';
import {
  CARD_UNIVERSITY_TITLE,
  CARD_UNIVERSITY_UNIVERSITY,
  CARD_UNIVERSITY_AWARD,
  CARD_UNIVERSITY_START_DATE,
  CARD_UNIVERSITY_GRADUATION_DATE,
  CARD_EMPLOYMENT_COMPANY,
  CARD_EMPLOYMENT_ADDRESS,
  CARD_EMPLOYMENT_STATUS,
  CARD_EMPLOYMENT_START_DATE,
  CARD_INSURANCE_PROVIDER,
  CARD_INSURANCE_CLASS,
  CARD_INSURANCE_POLICY_NUMBER,
  CARD_INSURANCE_END_DATE
} from '../../../../helpers/constants';
import './_style.scss';

const currentCredentialCard = {
  0: CredentialIDTemplate,
  1: CredentialData,
  2: CredentialData,
  3: CredentialData
};

const CreatedCredential = ({ currentCredential }) => {
  const { t } = useTranslation();
  const { user } = useContext(UserContext);

  const currentCredentialCellList = {
    0: undefined,
    1: {
      iconRight: 'images/icon-generic-university.svg',
      iconAlt: 'university credential logo',
      componentName: 'newUniversityCredential',
      cellTitle: { title: 'degreeName', value: CARD_UNIVERSITY_TITLE },
      cellList: [
        { title: 'universityName', value: CARD_UNIVERSITY_UNIVERSITY },
        { title: 'award', value: CARD_UNIVERSITY_AWARD },
        { title: 'fullName', value: `${user.firstName} ${user.lastName}` },
        {
          subList: [
            {
              title: 'startDate',
              value: moment.unix(CARD_UNIVERSITY_START_DATE).format('DD/MM/YYYY')
            },
            {
              title: 'graduationDate',
              value: moment.unix(CARD_UNIVERSITY_GRADUATION_DATE).format('DD/MM/YYYY')
            }
          ],
          className: 'DegreeDate'
        }
      ]
    },
    2: {
      credentialHeaderClassName: 'CredentialHeader EmploymentTheme',
      credentialTemplateClassName: 'CredentialTemplate',
      credentialContentClassName: 'CredentialContent EmploymentTheme',
      iconLeft: 'images/icon-generic-employment.svg',
      iconAlt: 'Atala.Inc logo',
      componentName: 'newEmploymentCredential',
      cellTitle: { title: 'companyName', value: CARD_EMPLOYMENT_COMPANY },
      cellList: [
        { title: 'employerAddress', value: CARD_EMPLOYMENT_ADDRESS },
        { title: 'employeeName', value: `${user.firstName} ${user.lastName}` },
        { title: 'employmentStatus', value: CARD_EMPLOYMENT_STATUS },
        {
          title: 'employmentStartDate',
          value: moment.unix(CARD_EMPLOYMENT_START_DATE).format('DD/MM/YYYY')
        }
      ]
    },
    3: {
      credentialHeaderClassName: 'CredentialHeader InsuranceTheme',
      credentialTemplateClassName: 'CredentialTemplate',
      credentialContentClassName: 'CredentialContent InsuranceTheme',
      iconRight: 'images/icon-generic-insurance.svg',
      iconAlt: 'insurance credential logo',
      componentName: 'newInsuranceCredential',
      cellTitle: { title: 'providerName', value: CARD_INSURANCE_PROVIDER },
      cellList: [
        { title: 'classOfInsurance', value: CARD_INSURANCE_CLASS },
        { title: 'policyNumber', value: CARD_INSURANCE_POLICY_NUMBER },
        { title: 'fullName', value: `${user.firstName} ${user.lastName}` },
        { title: 'policyEndDate', value: moment.unix(CARD_INSURANCE_END_DATE).format('DD/MM/YYYY') }
      ]
    }
  };

  const Card = currentCredentialCard[currentCredential];
  const cardContent = currentCredentialCellList[currentCredential];

  return (
    <div className="CreatedCredential">
      <h3>
        {t('landing.createdCredential.prevCredentialName') +
          t(`credential.credentialNames.CredentialType${currentCredential}`)}
      </h3>
      <div className="ContainerCredential">
        <Card {...user} {...cardContent} />
      </div>
    </div>
  );
};

CreatedCredential.propTypes = {
  currentCredential: PropTypes.number.isRequired
};

export default CreatedCredential;
