import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import StudentCreation from './StudentCreation';
import { withApi } from '../providers/withApi';
import { withRedirector } from '../providers/withRedirector';
import Logger from '../../helpers/Logger';
import { fromUnixToProtoDateFormatter } from '../../helpers/formatters';

const createBlankStudent = key => ({
  key,
  fullName: '',
  email: '',
  universityAssignedId: '',
  admissionDate: ''
});
const defaultStudentList = [createBlankStudent(0)];

const StudentCreationContainer = ({ api, redirector: { redirectToConnections } }) => {
  const { t } = useTranslation();
  const [students, setStudents] = useState(defaultStudentList);
  const [invalidStudents, setIndvalidStudents] = useState(false);

  const addNewStudent = () => {
    const { key = 0 } = _.last(students) || {};

    const newData = createBlankStudent(key + 1);
    const newDataSource = students.concat(newData);

    setStudents(newDataSource);
  };

  const editStudent = student => {
    const clonedStudents = [...students];

    const index = clonedStudents.findIndex(({ key }) => student.key === key);
    const item = clonedStudents[index];
    const editedStudent = Object.assign({}, item, student);

    clonedStudents.splice(index, 1, editedStudent);

    setStudents(clonedStudents);
  };

  const deleteStudent = key => {
    const filteredStudents = students.filter(({ key: studentKey }) => key !== studentKey);

    setStudents(filteredStudents);
  };

  const saveStudents = () => {
    if (!students.length) return redirectToConnections();

    const invalidStudent = students.reduce(
      (accumulator, { fullName, email, universityAssignedId, admissionDate }) => {
        const isInvalid = !fullName || !email || !universityAssignedId || !admissionDate;

        return accumulator || isInvalid;
      },
      false
    );

    setIndvalidStudents(invalidStudent);

    if (invalidStudent) return;

    const creationPromises = students.map(
      ({ fullName, email, universityAssignedId, admissionDate }) =>
        api.createStudent(
          universityAssignedId,
          fullName,
          email,
          fromUnixToProtoDateFormatter(admissionDate)
        )
    );

    Promise.all(creationPromises)
      .then(() => {
        message.success(t('studentCreation.success'));
        redirectToConnections();
      })
      .catch(error => {
        Logger.error('Error while creating student', error);
        message.error('Error while saving the student');
      });
  };

  const tableProps = {
    students,
    deleteStudent,
    editStudent
  };

  return (
    <StudentCreation
      saveStudent={addNewStudent}
      saveStudents={saveStudents}
      tableProps={tableProps}
      invalidStudents={invalidStudents}
    />
  );
};

StudentCreationContainer.propTypes = {
  api: PropTypes.shape({ createStudent: PropTypes.func }).isRequired,
  redirector: PropTypes.shape({ redirectToConnections: PropTypes.func }).isRequired
};

export default withApi(withRedirector(StudentCreationContainer));
