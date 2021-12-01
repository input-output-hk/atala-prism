import React from 'react';
import { Tabs } from 'antd';
import BulkImportTab from '../bulkImportTab/BulkImportTab';
import ManualImportTab from '../manualImportTab/ManualImportTab';
import { IMPORT_CONTACTS } from '../../../../helpers/constants';
import './_style.scss';

const { TabPane } = Tabs;

const ImportContactsTabs = () => {
  return (
    <div className="ImportContactsTabs">
      <Tabs defaultActiveKey="1">
        <TabPane tab="Bulk Import" key="1">
          <BulkImportTab />
        </TabPane>
        <TabPane tab="Import Manually" key="2">
          <ManualImportTab useCase={IMPORT_CONTACTS} />
          {/* TODO:: When the Credential Component is done, on the USE CASE put IMPORT_CREDENTIALS */}
        </TabPane>
      </Tabs>
    </div>
  );
};

export default ImportContactsTabs;
