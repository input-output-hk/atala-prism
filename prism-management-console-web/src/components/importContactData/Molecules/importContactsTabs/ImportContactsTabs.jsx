import React from 'react';
import { Tabs } from 'antd';
import BulkImportTab from '../bulkImportTab/BulkImportTab';
import ManualImportTab from '../manualImportTab/ManualImportTab';
import { useTranslation } from 'react-i18next';

import './_style.scss';
import { IMPORT_CONTACTS } from '../../../../helpers/constants';

const { TabPane } = Tabs;

const ImportContactsTabs = () => {
  const { t } = useTranslation();

  return (
    <div className="ImportContactsTabs">
      <Tabs defaultActiveKey="1">
        <TabPane tab="Bulk Import" key="1">
          <BulkImportTab />
        </TabPane>
        <TabPane tab="Import Manually" key="2">
          <ManualImportTab useCase={IMPORT_CONTACTS} />
          {/*CUANDO HAGA EL COMPONENTE DE CREDENTIALS EN EL USE CASE PONER IMPORT_CREDENTIALS*/}
        </TabPane>
      </Tabs>
    </div>
  );
};

export default ImportContactsTabs;
