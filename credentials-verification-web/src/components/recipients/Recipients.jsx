import React, { Fragment } from 'react';
import { Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import RecipientsFilter from './molecules/filter/RecipientsFilter';
import RecipientsTable from './organisms/table/RecipientsTable';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import noRecipients from '../../images/noRecipients.svg';
import Logger from '../../helpers/Logger';

import './_style.scss';

const RecipientsButtons = () => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        theme="theme-outline"
        buttonText={t('recipients.buttons.bulk')}
        onClick={() => Logger.info('placeholder function')}
        icon={<Icon type="plus" />}
      />
      <CustomButton
        theme="theme-secondary"
        buttonText={t('recipients.buttons.manual')}
        onClick={() => Logger.info('placeholder function')}
        icon={<Icon type="plus" />}
      />
    </div>
  );
};

const Recipients = ({ tableProps, filterProps }) => {
  const { t } = useTranslation();

  const emptyProps = {
    photoSrc: noRecipients,
    photoAlt: t('recipients.noRecipients.photoAlt'),
    title: t('recipients.noRecipients.title'),
    subtitle: t('recipients.noRecipients.subtitle')
  };

  return (
    <Fragment>
      <div className="ContentHeader">
        <h1>{t('recipients.title')}</h1>
        <RecipientsButtons />
      </div>
      <RecipientsFilter {...filterProps} />
      {tableProps.subjects.length ? (
        <RecipientsTable {...tableProps} />
      ) : (
        <EmptyComponent {...emptyProps} />
      )}
    </Fragment>
  );
};

const subjectShape = {
  avatar: PropTypes.string,
  name: PropTypes.string,
  identityNumber: PropTypes.number,
  admissionDate: PropTypes.number,
  email: PropTypes.string,
  status: PropTypes.oneOf(['PENDING_CONNECTION', 'CONNECTED', 'PENDING_INVITATION']),
  id: PropTypes.string
};

Recipients.propTypes = {
  tableProps: PropTypes.shape({
    subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
    subjectCount: PropTypes.number,
    offset: PropTypes.number,
    setOffset: PropTypes.func.isRequired,
    inviteHolder: PropTypes.func.isRequired
  }).isRequired,
  filterProps: PropTypes.shape({
    userId: PropTypes.string,
    setUserId: PropTypes.func.isRequired,
    name: PropTypes.string,
    setName: PropTypes.func.isRequired,
    status: PropTypes.string,
    setStatus: PropTypes.func.isRequired
  }).isRequired
};

export default Recipients;
