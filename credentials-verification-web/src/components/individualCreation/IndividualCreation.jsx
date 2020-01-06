import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Result } from 'antd';
import IndividualCreationTable from './Organisms/Table/IndividualCreationTable';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

const IndividualCreation = ({
  saveIndividual,
  saveIndividuals,
  tableProps,
  invalidIndividuals
}) => {
  const { t } = useTranslation();

  return (
    <div className="Wrapper">
      <div className="Header">
        <h1>{t('individualCreation.title')}</h1>
      </div>
      <div className="Content">
        {invalidIndividuals && (
          <Result
            status="error"
            title={t('individualCreation.cantSubmit')}
            subTitle={t('individualCreation.missingInfo')}
          />
        )}
        <IndividualCreationTable {...tableProps} />
      </div>
      <div className="ControlButtons">
        <CustomButton
          buttonProps={{ onClick: saveIndividuals, className: 'theme-secondary' }}
          buttonText={t('individualCreation.save')}
        />
        <CustomButton
          buttonProps={{ onClick: saveIndividual, className: 'theme-secondary' }}
          buttonText={t('individualCreation.newIndividual')}
        />
      </div>
    </div>
  );
};

IndividualCreation.propTypes = {
  saveIndividual: PropTypes.func.isRequired,
  saveIndividuals: PropTypes.func.isRequired,
  tableProps: PropTypes.shape().isRequired,
  invalidIndividuals: PropTypes.bool.isRequired
};

export default IndividualCreation;
