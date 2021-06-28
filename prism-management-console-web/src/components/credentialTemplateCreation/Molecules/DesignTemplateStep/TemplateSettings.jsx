import React from 'react';
import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import StylingSettings from './StylingSettings';
import ContentSettings from './ContentSettings';

const { TabPane } = Tabs;

const TemplateSettings = props => {
  const { t } = useTranslation();
  const tabs = {
    setStyle: {
      key: 0,
      title: t('credentialTemplateCreation.step2.style.title')
    },
    setContent: {
      key: 1,
      title: t('credentialTemplateCreation.step2.content.title')
    }
  };

  return (
    <Tabs defaultActiveKey={tabs.setStyle.key} centered>
      <TabPane tab={tabs.setStyle.title} key={tabs.setStyle.key}>
        <StylingSettings />
      </TabPane>
      <TabPane tab={tabs.setContent.title} key={tabs.setContent.key}>
        <ContentSettings />
      </TabPane>
    </Tabs>
  );
};

TemplateSettings.propTypes = {};

export default TemplateSettings;
