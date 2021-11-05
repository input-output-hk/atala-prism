import React, { useState } from 'react';
import PropTypes from 'prop-types';
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
      <button type="button" onClick={showModal} className="progressChecklist">
        <Progress percent={percent} />
        <p>{text}</p>
      </button>

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

ProgressChecklist.propTypes = {
  text: PropTypes.string.isRequired,
  percent: PropTypes.number.isRequired
};
export default ProgressChecklist;
