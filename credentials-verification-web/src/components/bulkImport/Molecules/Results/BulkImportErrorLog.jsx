import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Table, Tag } from 'antd';
import Clip from '../../../../images/Clip.png';
import greenCheck from '../../../../images/greenCheck.svg';
import redCross from '../../../../images/redCross.svg';
import { getColName, getRowNumber } from '../../../../helpers/fileHelpers';
import GenericFooter from '../../../common/Molecules/GenericFooter/GenericFooter';
import './_style.scss';

const getCellNumber = (row, col) => `Cell # ${getColName(col)}${getRowNumber(row)}`;

const BulkImportErrorLog = ({ fileData: { fileObj }, validationErrors, returnToUploadStep }) => {
  const { t } = useTranslation();

  const columns = [
    {
      title: t('bulkImport.errorLog.headers.cellNumber'),
      dataIndex: 'celNumber',
      key: 'celNumber',
      width: 200
    },
    {
      title: t('bulkImport.errorLog.headers.errorDescription'),
      dataIndex: 'errorDescription',
      key: 'errorDescription',
      width: 400
    },
    {
      title: t('bulkImport.errorLog.headers.state'),
      key: 'state',
      dataIndex: 'state',
      width: 100,
      render: state => {
        const isError = state === 'error';
        return (
          <Tag color="volcano" key={isError}>
            {isError ? 'X' : ''}
          </Tag>
        );
      }
    }
  ];

  const invalidRows = validationErrors.filter(row => row.length);

  const errorsTableData = invalidRows.flat().map((invalidRow, idx) => ({
    key: idx,
    celNumber: getCellNumber(invalidRow.row.index, invalidRow.col.index),
    errorDescription: t(`bulkImport.errorLog.errors.${invalidRow.error}`, {
      headerName: invalidRow.col.name,
      fieldContent: invalidRow.col.content,
      expectedHeaderPosition: getColName(invalidRow.col.expectedIndex),
      headerNameErrorDescription: invalidRow.col.name
        ? t('bulkImport.errorLog.extraHeaderErrorDescriptionWithName', {
            headerName: invalidRow.col.name
          })
        : t('bulkImport.errorLog.extraHeaderErrorDescriptionWithNoName')
    }),
    state: 'error'
  }));

  return (
    <>
      <div className="errorLogSection">
        <div className="title">
          <div>
            <h1>{t('bulkImport.errorLog.title')}</h1>
            <h3>{t('bulkImport.errorLog.info')}</h3>
          </div>
        </div>
        <div className="table">
          <Table
            scroll={{ y: '36vh' }}
            pagination={false}
            columns={columns}
            dataSource={errorsTableData}
          />
          <div className="result">
            <div className="leftResults">
              <div className="usersFile">
                <img src={Clip} alt="file-icon" />
                <p>{fileObj.name}</p>
              </div>
            </div>
            <div className="rightResults">
              <div className="processedRows">
                <p>
                  <strong>{t('bulkImport.errorLog.processedRows')}</strong>
                  {` ${validationErrors.length}`}
                </p>
              </div>
              <div className="okRows">
                <img src={greenCheck} alt="ok" />
                <p>
                  <strong>{`${validationErrors.length - invalidRows.length} `}</strong>
                  {t('bulkImport.errorLog.correctRows')}
                </p>
              </div>
              <div className="xRows">
                <img src={redCross} alt="not-ok" />
                <p>
                  <strong>{`${invalidRows.length} `}</strong>
                  {t('bulkImport.errorLog.incorrectRows')}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
      <GenericFooter
        previous={returnToUploadStep}
        labels={{ previous: t('bulkImport.errorLog.uploadAgain') }}
      />
    </>
  );
};

BulkImportErrorLog.propTypes = {
  fileData: PropTypes.shape({
    fileObj: PropTypes.shape({ name: PropTypes.string })
  }).isRequired,
  validationErrors: PropTypes.arrayOf(
    PropTypes.arrayOf({
      error: PropTypes.string,
      row: PropTypes.number,
      col: PropTypes.number
    })
  ).isRequired,
  returnToUploadStep: PropTypes.func.isRequired
};

export default BulkImportErrorLog;
