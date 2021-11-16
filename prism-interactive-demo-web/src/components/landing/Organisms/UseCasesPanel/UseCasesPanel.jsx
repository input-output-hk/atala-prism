import React from 'react';
import { Tabs } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';

const UseCasesPanel = () => {
  const { t } = useTranslation();

  const { TabPane } = Tabs;

  function callback(key) {
    console.log(key);
  }

  return (
    <div className="UseCasesPanel">
      <div className="TextContainer">
        <h1>{t('landing.useCasesPanel.title')}</h1>
        <h3>{t('landing.useCasesPanel.subtitle')}</h3>
      </div>
      <div className="UseCasesContainer">
        <Tabs defaultActiveKey="1" onChange={callback}>
          <TabPane tab="Education" key="1">
            <div className="TabContent">
              <div className="TabImage">
                <img src="/images/illustration-education.svg" alt={t('atalaLogo')} />
              </div>
              <div className="TabText">
                <p>{t('landing.useCasesPanel.listItem1')}</p>
                <p>{t('landing.useCasesPanel.listItem2')}</p>
                <p>{t('landing.useCasesPanel.listItem3')}</p>
                <p>{t('landing.useCasesPanel.listItem4')}</p>
                <p>{t('landing.useCasesPanel.listItem5')}</p>
                <p>{t('landing.useCasesPanel.listItem6')}</p>
              </div>
            </div>
          </TabPane>
          <TabPane tab="Government" key="2">
            <div className="TabContent">
              <div className="TabImage">
                <img src="/images/illustration-goverment.svg" alt={t('atalaLogo')} />
              </div>
              <div className="TabText">
                <p>{t('landing.useCasesPanel.listItem1')}</p>
                <p>{t('landing.useCasesPanel.listItem7')}</p>
                <p>{t('landing.useCasesPanel.listItem8')}</p>
                <p>{t('landing.useCasesPanel.listItem3')}</p>
                <p>{t('landing.useCasesPanel.listItem9')}</p>
                <p>{t('landing.useCasesPanel.listItem10')}</p>
              </div>
            </div>
          </TabPane>
          <TabPane tab="Health" key="3">
            <div className="TabContent">
              <div className="TabImage">
                <img src="/images/illustration-health.svg" alt={t('atalaLogo')} />
              </div>
              <div className="TabText">
                <p>{t('landing.useCasesPanel.listItem1')}</p>
                <p>{t('landing.useCasesPanel.listItem8')}</p>
                <p>{t('landing.useCasesPanel.listItem3')}</p>
                <p>{t('landing.useCasesPanel.listItem11')}</p>
                <p>{t('landing.useCasesPanel.listItem12')}</p>
                <p>{t('landing.useCasesPanel.listItem13')}</p>
              </div>
            </div>
          </TabPane>
          <TabPane tab="Enterprise" key="4">
            <div className="TabContent">
              <div className="TabImage">
                <img src="/images/illustration-enterprise.svg" alt={t('atalaLogo')} />
              </div>
              <div className="TabText">
                <p>{t('landing.useCasesPanel.listItem1')}</p>
                <p>{t('landing.useCasesPanel.listItem14')}</p>
                <p>{t('landing.useCasesPanel.listItem3')}</p>
                <p>{t('landing.useCasesPanel.listItem10')}</p>
                <p>{t('landing.useCasesPanel.listItem15')}</p>
                <p>{t('landing.useCasesPanel.listItem16')}</p>
              </div>
            </div>
          </TabPane>
          <TabPane tab="Finance" key="5">
            <div className="TabContent">
              <div className="TabImage">
                <img src="/images/illustration-finance.svg" alt={t('atalaLogo')} />
              </div>
              <div className="TabText">
                <p>{t('landing.useCasesPanel.listItem1')}</p>
                <p>{t('landing.useCasesPanel.listItem17')}</p>
                <p>{t('landing.useCasesPanel.listItem18')}</p>
                <p>{t('landing.useCasesPanel.listItem19')}</p>
                <p>{t('landing.useCasesPanel.listItem20')}</p>
                <p>{t('landing.useCasesPanel.listItem21')}</p>
              </div>
            </div>
          </TabPane>
          <TabPane tab="Travel" key="6">
            <div className="TabContent">
              <div className="TabImage">
                <img src="/images/illustration-travel.svg" alt={t('atalaLogo')} />
              </div>
              <div className="TabText">
                <p>{t('landing.useCasesPanel.listItem22')}</p>
                <p>{t('landing.useCasesPanel.listItem23')}</p>
                <p>{t('landing.useCasesPanel.listItem24')}</p>
                <p>{t('landing.useCasesPanel.listItem25')}</p>
                <p>{t('landing.useCasesPanel.listItem26')}</p>
                <p>{t('landing.useCasesPanel.listItem27')}</p>
              </div>
            </div>
          </TabPane>
          <TabPane tab="Social" key="7">
            <div className="TabContent">
              <div className="TabImage">
                <img src="/images/illustration-social.svg" alt={t('atalaLogo')} />
              </div>
              <div className="TabText">
                <p>{t('landing.useCasesPanel.listItem28')}</p>
                <p>{t('landing.useCasesPanel.listItem29')}</p>
                <p>{t('landing.useCasesPanel.listItem30')}</p>
                <p>{t('landing.useCasesPanel.listItem31')}</p>
                <p>{t('landing.useCasesPanel.listItem32')}</p>
                <p>{t('landing.useCasesPanel.listItem33')}</p>
              </div>
            </div>
          </TabPane>
        </Tabs>
      </div>
    </div>
  );
};

export default UseCasesPanel;
