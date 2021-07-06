import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { Result, Select } from 'antd';
import StudentCreationTable from './Organisms/Table/StudentCreationTable';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

import './_style.scss';
import { customLowerCase } from '../../helpers/genericHelpers';

const getLinker = (firstField, lastWord, t) => {
  if (firstField) return '';
  if (lastWord) return ` ${t('commonWords.and')}`;
  return ',';
};

const appendLinkedtoAcum = (accumulator, index, fieldsCount, t) => {
  const linker = getLinker(!accumulator, index + 1 === fieldsCount, t);
  return `${accumulator}${linker}`;
};

const StudentCreation = ({
  saveStudent,
  saveStudents,
  tableProps,
  invalidFields,
  canAddMore,
  groups,
  group,
  setGroup
}) => {
  const { t } = useTranslation();

  const getSubtitle = () => {
    const part1 = t('studentCreation.missingInfoPart1');
    const part2 = t('studentCreation.missingInfoPart2', {
      isOrAre: t(`commonWords.${invalidFields.length > 1 ? 'are' : 'is'}`)
    });

    const missingFields = customLowerCase(
      invalidFields.reduce(
        (accumulator, currentField, index) =>
          `${appendLinkedtoAcum(accumulator, index, invalidFields.length, t)} ${t(
            `studentCreation.table.${currentField}`
          )}`,
        ''
      )
    );

    return `${part1}${missingFields}${part2}`;
  };

  return (
    <div className="Wrapper">
      <div className="StudentCreationHeader">
        <h1>{t('studentCreation.title')}</h1>
        <CustomButton
          buttonProps={{
            onClick: saveStudent,
            className: 'theme-secondary',
            disabled: canAddMore
          }}
          buttonText={t('studentCreation.newStudent')}
          icon={<PlusOutlined />}
        />
      </div>
      <div className="Content">
        <div className="GroupSelection">
          <p>{t('studentCreation.selectGroup')}</p>
          <Select onSelect={setGroup} value={group}>
            {groups.map(({ name }) => (
              <Select.Option key={name}>{name}</Select.Option>
            ))}
          </Select>
        </div>
        {!!invalidFields.length && (
          <Result status="error" title={t('studentCreation.cantSubmit')} subTitle={getSubtitle()} />
        )}
        <StudentCreationTable {...tableProps} />
      </div>
      <div className="StudentCreationFooter">
        <CustomButton
          buttonProps={{ onClick: saveStudents, className: 'theme-outline', disabled: canAddMore }}
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
  invalidFields: PropTypes.arrayOf().isRequired,
  canAddMore: PropTypes.bool.isRequired,
  groups: PropTypes.arrayOf(PropTypes.shape()).isRequired,
  setGroup: PropTypes.func.isRequired,
  group: PropTypes.shape().isRequired
};

export default StudentCreation;
