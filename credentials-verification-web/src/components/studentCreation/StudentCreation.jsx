import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Result, Icon } from 'antd';
import StudentCreationTable from './Organisms/Table/StudentCreationTable';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const StudentCreation = ({ saveStudent, saveStudents, tableProps, invalidStudents }) => {
  const { t } = useTranslation();

  return (
    <div className="Wrapper">
      <div className="StudentCreationHeader">
        <h1>{t('studentCreation.title')}</h1>
        <CustomButton
          buttonProps={{ onClick: saveStudent, className: 'theme-secondary' }}
          buttonText={t('studentCreation.newStudent')}
          icon={<Icon type="plus" />}
        />
      </div>
      <div className="Content">
        {invalidStudents && (
          <Result
            status="error"
            title={t('studentCreation.cantSubmit')}
            subTitle={t('studentCreation.missingInfo')}
          />
        )}
        <StudentCreationTable {...tableProps} />
      </div>
      <div className="StudentCreationFooter">
        <CustomButton
          buttonProps={{ onClick: saveStudents, className: 'theme-outline' }}
          buttonText={t('studentCreation.save')}
        />
      </div>
    </div>
  );
};

StudentCreation.propTypes = {
  saveStudent: PropTypes.func.isRequired,
  saveStudents: PropTypes.func.isRequired,
  tableProps: PropTypes.shape().isRequired,
  invalidStudents: PropTypes.bool.isRequired
};

export default StudentCreation;
