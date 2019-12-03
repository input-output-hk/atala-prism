import React, { Fragment, useState } from 'react';
import { Row, Icon, Drawer } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialsFilter from './Molecules/Filters/CredentialsFilter/CredentialsFilter';
import CredentialsTable from './Organisms/Tables/CredentialsTable/CredentialsTable';
import { credentials, categories, groups } from '../../APIs/__mocks__/credentials';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import noCredentials from '../../images/noCredentials.svg';
import CredentialData from '../common/Atoms/CredentialData/CredentialData';
import { drawerWidth } from '../../helpers/constants';

import './_style.scss';
import CredentialDetail from '../common/Organisms/Detail/CredentialDetail';

const CredentialsButtons = () => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonText={t('credentials.actions.createCredential')}
        theme="theme-outline"
        icon={<Icon type="plus" />}
      />
    </div>
  );
};

const Credentials = ({ showEmpty, tableProps, filterProps }) => {
  const { t } = useTranslation();
  const [currentCredential, setCurrentCredential] = useState({});
  const [showDrawer, setShowDrawer] = useState(false);

  const emptyProps = {
    photoSrc: noCredentials,
    photoAlt: t('credentials.noCredentials.photoAlt'),
    title: t('credentials.noCredentials.title'),
    subtitle: t('credentials.noCredentials.subtitle'),
    button: (
      <CustomButton
        buttonText={t('credentials.actions.createCredential')}
        theme="theme-secondary"
        icon={<Icon type="plus" />}
      />
    )
  };

  const showCredentialData = ({ title, student, admissionDate }) => {
    const credentialToShow = {
      title,
      university: '', // For the moment the credential does not have this
      award: '', // For the moment the credential does not have this
      student: student ? student.name : '',
      startDate: admissionDate,
      graduationDate: 34 // For the moment the credential does not have this
    };
    setCurrentCredential(credentialToShow);
    setShowDrawer(true);
  };

  const expandedTableProps = Object.assign({}, tableProps, { onView: showCredentialData });

  return (
    <div className="Wrapper PageContainer">
      <CredentialDetail
        drawerInfo={{
          title: t('credentials.detail.title'),
          onClose: () => setShowDrawer(false),
          visible: showDrawer
        }}
        credentialData={currentCredential}
      />
      <div className="ContentHeader">
        <h1>{t('credentials.title')}</h1>
        <CredentialsButtons />
      </div>
      <CredentialsFilter {...filterProps} />

      <Row>
        {tableProps.credentialCount ? (
          <CredentialsTable {...expandedTableProps} />
        ) : (
          <EmptyComponent {...emptyProps} />
        )}
      </Row>
    </div>
  );
};

Credentials.defaultProps = {
  showEmpty: false
};

const subjectShape = {
  credentialId: '',
  name: '',
  credentialType: '',
  category: '',
  group: ''
};

Credentials.propTypes = {
  tableProps: PropTypes.shape({
    subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
    subjectCount: PropTypes.number,
    offset: PropTypes.number,
    setOffset: PropTypes.func.isRequired
  }).isRequired,
  filterProps: PropTypes.shape({
    credentialId: PropTypes.string,
    setCredentialId: PropTypes.func.isRequired,
    name: PropTypes.string,
    setName: PropTypes.func.isRequired,
    credentialTypes: PropTypes.oneOf(credentials),
    credentialType: PropTypes.string,
    setCredentialType: PropTypes.func.isRequired,
    categories: PropTypes.oneOf(categories),
    category: PropTypes.string,
    setCategory: PropTypes.func.isRequired,
    groups: PropTypes.oneOf(groups),
    group: PropTypes.string,
    setGroup: PropTypes.func.isRequired
  }).isRequired,
  showEmpty: PropTypes.bool
};

export default Credentials;
