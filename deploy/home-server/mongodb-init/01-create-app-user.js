const databaseName = process.env.SPRING_DATA_MONGODB_DATABASE || 'edusecure';
const appUsername = process.env.MONGODB_APP_USERNAME;
const appPassword = process.env.MONGODB_APP_PASSWORD;

if (!appUsername || !appPassword) {
  throw new Error('MONGODB_APP_USERNAME and MONGODB_APP_PASSWORD must be set before MongoDB initialisation.');
}

const targetDb = db.getSiblingDB(databaseName);
const existingUser = targetDb.getUser(appUsername);

if (existingUser) {
  print(`MongoDB application user '${appUsername}' already exists in database '${databaseName}'.`);
} else {
  targetDb.createUser({
    user: appUsername,
    pwd: appPassword,
    roles: [
      {
        role: 'readWrite',
        db: databaseName,
      },
    ],
  });

  print(`Created MongoDB application user '${appUsername}' in database '${databaseName}'.`);
}

