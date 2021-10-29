import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import CredentialData from '../../../common/Atoms/CredentialData/CredentialData';
import { UserContext } from '../../../providers/userContext';
import CredentialIDTemplate from '../../Molecules/CredentialIdTemplate/CredentialIdTemplate';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import {
  CARD_UNIVERSITY_TITLE,
  CARD_UNIVERSITY_UNIVERSITY,
  CARD_UNIVERSITY_AWARD,
  CARD_EMPLOYMENT_COMPANY,
  CARD_EMPLOYMENT_STATUS,
  CARD_INSURANCE_PROVIDER,
  CARD_INSURANCE_CLASS,
  CARD_INSURANCE_POLICY_NUMBER,
  INSURANCE_POLICY
} from '../../../../../helpers/constants';

import './_style.scss';

const currentCredentialCard = {
  0: CredentialIDTemplate,
  1: CredentialData,
  2: CredentialData,
  3: CredentialData
};

const prevCredentialMessage = [
  'prevCredentialName',
  'prevCredentialName',
  'prevProofOfEmployementName',
  'prevCredentialName'
];

const CreatedCredential = ({ confirmSuccessCredential, currentCredential }) => {
  const { t } = useTranslation();
  const { user } = useContext(UserContext);
  const { firstName } = user;
  const currentCredentialCellList = {
    0: undefined,
    1: {
      iconRight: '/images/icon-generic-university.svg',
      iconAlt: 'university credential logo',
      componentName: 'newUniversityCredential',
      cellTitle: { title: 'universityName', value: CARD_UNIVERSITY_UNIVERSITY },
      cellList: [
        { title: 'fullName', value: firstName },
        { title: 'degreeName', value: CARD_UNIVERSITY_TITLE },
        { title: 'award', value: CARD_UNIVERSITY_AWARD },
        {
          title: 'graduationDate',
          value: moment().format('YYYY-MM-DD')
        }
      ]
    },
    2: {
      credentialHeaderClassName: 'CredentialHeader EmploymentTheme',
      credentialTemplateClassName: 'CredentialTemplate',
      credentialContentClassName: 'CredentialContent EmploymentTheme',
      iconLeft: '/images/icon-generic-employment.svg',
      iconAlt: 'Atala.Inc logo',
      componentName: 'newEmploymentCredential',
      cellTitle: { title: 'companyName', value: CARD_EMPLOYMENT_COMPANY },
      cellList: [
        { title: 'employeeName', value: firstName },
        { title: 'employmentStatus', value: CARD_EMPLOYMENT_STATUS },
        {
          title: 'employmentStartDate',
          value: moment().format('YYYY-MM-DD')
        }
      ]
    },
    3: {
      credentialHeaderClassName: 'CredentialHeader InsuranceTheme',
      credentialTemplateClassName: 'CredentialTemplate',
      credentialContentClassName: 'CredentialContent InsuranceTheme',
      iconRight: '/images/icon-generic-insurance.svg',
      iconAlt: 'insurance credential logo',
      componentName: 'newInsuranceCredential',
      cellTitle: { title: 'providerName', value: CARD_INSURANCE_PROVIDER },
      cellList: [
        { title: 'fullName', value: firstName },
        { title: 'classOfInsurance', value: CARD_INSURANCE_CLASS },
        { title: 'policyNumber', value: CARD_INSURANCE_POLICY_NUMBER },
        {
          title: 'policyEndDate',
          value: moment()
            .add(1, 'y')
            .format('YYYY-MM-DD')
        }
      ]
    }
  };

  const Card = currentCredentialCard[currentCredential];
  const cardContent = currentCredentialCellList[currentCredential];

  return (
    <div>
      <div className="CreatedCredential">
        <h3>{t(`landing.createdCredential.${prevCredentialMessage[currentCredential]}`)}</h3>
        <div className="ContainerCredential">
          <Card {...user} {...cardContent} />
        </div>
      </div>
      {currentCredential !== INSURANCE_POLICY && (
        <div className="centeredButton">
          <CustomButton
            buttonProps={{ onClick: confirmSuccessCredential, className: 'theme-secondary' }}
            buttonText={t('credential.finishInfo.finished')}
          />
        </div>
      )}
    </div>
  );
};

CreatedCredential.propTypes = {
  currentCredential: PropTypes.number.isRequired
};

export default CreatedCredential;
