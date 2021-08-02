import React from 'react';
import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import StylingSettings from './StylingSettings';
import ContentSettings from './ContentSettings';
import '../../_style.scss';

const { TabPane } = Tabs;

const TemplateSettings = () => {
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
    <Tabs className="templateTab" defaultActiveKey={tabs.setStyle.key} centered>
      <TabPane tab={tabs.setStyle.title} key={tabs.setStyle.key}>
        <StylingSettings />
      </TabPane>
      <TabPane forceRender tab={tabs.setContent.title} key={tabs.setContent.key}>
        <ContentSettings />
      </TabPane>
    </Tabs>
  );
};

export default TemplateSettings;
