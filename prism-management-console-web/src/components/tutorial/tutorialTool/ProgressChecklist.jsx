import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Progress, Modal } from 'antd';
import success from '../../../images/tutSuccess.svg';
import './_style.scss';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

const ProgressChecklist = ({ text, percent }) => {
  const { t } = useTranslation();

  const [isModalVisible, setIsModalVisible] = useState(false);

  const showModal = () => {
    setIsModalVisible(true);
  };

  const handleOk = () => {
    setIsModalVisible(false);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };
  return (
    <>
      <a onClick={showModal} className="progressChecklist">
        {/* this should link to each page now it links to the complete section modal but it should only do this once completed the setp */}
        <Progress percent={percent} />
        <p>{text}</p>
      </a>

      <Modal
        className="tutorialSuccessModal"
        visible={isModalVisible}
        onOk={handleOk}
        onCancel={handleCancel}
      >
        <div>
          <img src={success} alt="tutorialMessage" />
        </div>
        <div className="modalText">
          <h2>{t('tutorial.modal.basicStep')}</h2>
          <p>{t('tutorial.modal.description')}</p>
        </div>
        <div className="buttonModal">
          <CustomButton
            buttonProps={{
              className: 'theme-outline'
            }}
            buttonText={t('tutorial.modal.buttonText.skip')}
          />
          <CustomButton
            buttonProps={{
              className: 'theme-primary'
            }}
            buttonText={t('tutorial.modal.buttonText.continue')}
          />
        </div>
      </Modal>
    </>
  );
};

export default ProgressChecklist;
